package fr.partipirate.discord.bots.congressus.commands.dj;

import java.awt.Color;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import fr.partipirate.discord.bots.congressus.CongressusBot;
import fr.partipirate.discord.bots.congressus.commands.ICommand;
import fr.partipirate.discord.bots.congressus.commands.radio.MusicBrainzTrackInfo;
import fr.partipirate.discord.bots.congressus.commands.radio.RadioHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.User;

public class NowPlayingCommand extends ADJCommand implements ICommand {
//    private static final String CD = "\uD83D\uDCBF";
//    private static final String DVD = "\uD83D\uDCC0";
//    private static final String MIC = "\uD83C\uDFA4 **|>** ";

//    private static final String QUEUE_TITLE = "__%s has added %d new track%s to the Queue:__";
//    private static final String QUEUE_DESCRIPTION = "%s **|>**  %s\n%s\n%s %s";
//    private static final String QUEUE_INFO = "Info about the Queue: (Size - %d)";
//    private static final String ERROR = "Error while loading \"%s\"";

	private CongressusBot bot;

	public NowPlayingCommand(CongressusBot bot) {
		this.bot = bot;
	}

	@Override
	public String getKeyWord() {
		return "nowplaying";
	}

	@Override
	public boolean canDoCommand(User user, Guild guild) {
		return true;
	}

	@Override
	public void doCommand(User user, MessageChannel channel, Guild guild, String[] commandParts) {
		if (channel instanceof PrivateChannel) {
        	channel.sendMessage("*Commande à faire publiquement*").complete();
		}
		else if (getPlayer(guild).getPlayingTrack() == null) { // No song is playing
        	channel.sendMessage("Aucune musique ").complete();
        } 
        else {

            AudioTrack track = getPlayer(guild).getPlayingTrack();

            // Create the EmbedBuilder instance
            EmbedBuilder eb = new EmbedBuilder();
            
            eb.setColor(new Color(0x41D55F));
            
            MusicBrainzTrackInfo trackInfo = RadioHelper.searchTrackInfo(track.getInfo().title, track.getInfo().author) ;
            
            if(trackInfo == null) {
            	
            	eb.setTitle(track.getInfo().title) ;
    			eb.setAuthor(track.getInfo().author) ;
    			
            }
            else {
            	
            	if (trackInfo.getRecordingName() == null) {
            		eb.setTitle(track.getInfo().title) ;
            	}
            	else {
            		if (trackInfo.getRecordingURL() == null) {
            			eb.setTitle(trackInfo.getRecordingName()) ;
            		}
            		else {
            			eb.setTitle(trackInfo.getRecordingName(), trackInfo.getRecordingURL()) ;
            		}
            	}
            	
            	if (trackInfo.getArtistName() == null) {
            		if (trackInfo.getArtistURL() == null) {
            			eb.setTitle(trackInfo.getArtistName()) ;
            		}
            		else {
            			eb.setTitle(trackInfo.getArtistName(), trackInfo.getArtistURL()) ;
            		}
            		eb.setTitle(track.getInfo().title) ;
            	}
            	else {
            		eb.setTitle(trackInfo.getArtistName()) ;
            	}
            	
            	if (trackInfo.getCoverURL() != null) {
            		eb.setImage(trackInfo.getCoverURL()) ;
            	}
            	
            	
            }


			long timeDuration = track.getDuration() ;
			long timePosition = track.getPosition() ;
				
			eb.addField("", getTimestamp(timePosition) + " / " + getTimestamp(timeDuration), true);
				
			channel.sendMessage(eb.build()).complete();
        }
	}

	@Override
	public String getCommandHelp() {
		return "Donne les informations de la piste courante, s'il y en a une.";
	}

	private AudioPlayer getPlayer(Guild guild) {
		return bot.getGuildAudioPlayer(guild).getPlayer();
	}
}