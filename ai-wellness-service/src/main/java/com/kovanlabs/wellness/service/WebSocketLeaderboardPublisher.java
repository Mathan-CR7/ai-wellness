package com.kovanlabs.wellness.service;

import com.kovanlabs.wellness.dto.team.TeamLeaderboardResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketLeaderboardPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketLeaderboardPublisher.class);
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketLeaderboardPublisher(SimpMessagingTemplate messagingTemplate)
    {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishLeaderboardUpdate(Long teamId, TeamLeaderboardResponse leaderboard)
    {
        String destination = "/topic/leaderboard/" + teamId;
        log.info("Publishing real-time team leaderboard update to topic: {}", destination);
        messagingTemplate.convertAndSend(destination, leaderboard);
    }

    public void publishLeaderBoardUpdate(Long teamId, TeamLeaderboardResponse leaderboard)
    {
        String destination = "/topic/leaderboard/" + teamId;
        log.info("Publishing real-time team leaderboard update to topic: {}", destination);
        messagingTemplate.convertAndSend(destination, leaderboard);
    }

    public void publishChallengeLeaderboardUpdate(Long challengeId, Object leaderboard)
    {
        String destination = "/topic/challenges/" + challengeId + "/leaderboard";
        log.info("Publishing real-time challenge leaderboard update to topic: {}", destination);
        messagingTemplate.convertAndSend(destination, leaderboard);
    }
}
