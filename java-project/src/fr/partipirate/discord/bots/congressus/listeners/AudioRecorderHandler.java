package fr.partipirate.discord.bots.congressus.listeners;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.collections4.map.HashedMap;

import fr.partipirate.discord.bots.congressus.Configuration;
import net.dv8tion.jda.core.audio.AudioReceiveHandler;
import net.dv8tion.jda.core.audio.CombinedAudio;
import net.dv8tion.jda.core.audio.UserAudio;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;

public class AudioRecorderHandler implements AudioReceiveHandler {

	private static byte[] SILENCE_DATA = new byte[3840]; // 3840 bytes of 0
	private static SimpleDateFormat DT_FORMAT = new SimpleDateFormat("yyyyMMdd-HHmm");
	private long start;
	private boolean recording = false;
	private FileOutputStream fis = null;
	private Map<String, FileOutputStream> userFileOutputStreams = null;
	private String filename = null;
	private VoiceChannel voicedChannel;
	private long numberOfCycles;

	public AudioRecorderHandler(VoiceChannel voicedChannel) {
		this.voicedChannel = voicedChannel;
	}

	public String getRecorderDir() {
		String recorderDir = (Configuration.getInstance().OPTIONS.get("recorder") != null) ? (Configuration.getInstance().OPTIONS.get("recorder").get("audio") != null ? Configuration.getInstance().OPTIONS.get("recorder").get("audio") : "")  : "";

		System.out.println(recorderDir);

		return recorderDir;
	}

	public String getRelativeFilePath(String extension) {
		File file = new File(getFilename(extension));
		String filename = file.getName();
		
		return getRecorderDir() + filename;
	}

	public String getFilename(String extension) {
		return filename + "." + extension;
	}
	
	public boolean isRecording() {
		return recording;
	}

	public void startRecording() {
		this.start = System.currentTimeMillis();
		this.numberOfCycles = 0;
		this.userFileOutputStreams = new HashedMap<String, FileOutputStream>();
		this.recording = true;

		// open outputstream
		try {
			File file = new File(getRecorderDir() + this.voicedChannel.getName() + "-" + DT_FORMAT.format(new Date()));
			filename = file.getAbsolutePath();
			fis = new FileOutputStream(getFilename("pcm"));
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
//		System.out.println("Start recording");
	}
	
	public long endRecording() {
		long end = System.currentTimeMillis();

		this.recording = false;

		// close outputstream
		try {
			fis.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

//		System.out.println("Stop recording");

		return end - start;
	}
	
	@Override
	public boolean canReceiveCombined() {
//		System.out.println("Recording :" + this.recording);

		return this.recording;
	}

	@Override
	public boolean canReceiveUser() {
		return this.recording;
	}

	@Override
	public void handleCombinedAudio(CombinedAudio combinedAudio) {
		byte[] data = combinedAudio.getAudioData(1.0);
		
		try {
			fis.write(data);
		}
		catch (Exception e) {
			e.printStackTrace();
		}

//		System.out.println(voicedChannel.getMembers());
		for(String filename : userFileOutputStreams.keySet()) {
			boolean foundUser = false;
			for (Member member : voicedChannel.getMembers()) {
				User user = member.getUser();

				String userFileName = getFilename(user.getName() + "." + user.getDiscriminator() + ".pcm");
				if (userFileName.equals(filename)) {
					boolean isSpeaking = false;
					for (User speakingUser : combinedAudio.getUsers()) {
						//if member is speaking, is in the user list
						if (user.equals(speakingUser)) {
							isSpeaking = true;
							break;
						}
					}
	
					if (!isSpeaking) {
						addUserAudioData(user, SILENCE_DATA);
					}

					foundUser = true;
					break;
				}
			}

			// The user is no more present
			if (!foundUser) {
				try {
					userFileOutputStreams.get(filename).write(SILENCE_DATA);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		numberOfCycles++;
	}

	@Override
	public void handleUserAudio(UserAudio userAudio) {
		byte[] data = userAudio.getAudioData(1.0);
		addUserAudioData(userAudio.getUser(), data);
	}

	private void addUserAudioData(User user, byte[] data) {
		String userFileName = getFilename(user.getName() + "." + user.getDiscriminator() + ".pcm");
		try {
			FileOutputStream userFis = userFileOutputStreams.get(userFileName);
			if (userFis == null) {
				userFis = new FileOutputStream(userFileName);
				userFileOutputStreams.put(userFileName, userFis);

				// init the silence time before the user arrived
				for(long index = 0; index < numberOfCycles; ++index) {
					userFis.write(SILENCE_DATA);
				}
			}
			userFis.write(data);
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Map<String, FileOutputStream> getUserFileOutputStreams() {
		return userFileOutputStreams;
	}
	
	public VoiceChannel getVoiceChannel() {
		return this.voicedChannel;
	}
}