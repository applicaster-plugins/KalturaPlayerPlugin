//
//  Player+Progress.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/2/18.
//  Copyright © 2018 Applicaster. All rights reserved.
//

import Foundation
import CoreMedia
import AVKit
import AVFoundation
import ZappCore

extension Player {
    func sendProgressUpdate() {
        guard let video = player?.currentItem,
        video.status == .readyToPlay else {
            return
        }
        let playerDuration = playerItemDuration()
        if CMTIME_IS_INVALID(playerDuration) {
            return
        }
        
        if let currentTime = player?.currentTime {
            let duration = CMTimeGetSeconds(playerDuration)
            let currentTimeSecs = CMTimeGetSeconds(currentTime())
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "RCTVideo_progress"),
                                            object: nil,
                                            userInfo: ["progress": currentTimeSecs / duration])
            FacadeConnector.connector?.playerDependant?.playerProgressUpdate(player: self,
                                                                                currentTime: TimeInterval(currentTimeSecs),
                                                                                duration: TimeInterval(duration))
            if currentTimeSecs >= 0,
                onVideoProgress != nil {
                onVideoProgress?([
                    "currentTime": CMTimeGetSeconds(currentTime()),
                     "playableDuration": calculatePlayableDuration(),
                     "atValue": currentTime().value,
                       "atTimescale": currentTime().timescale,
                        "target": reactTag ?? NSNull(),
                         "seekableDuration": calculateSeekableDuration(),
                    ])
            }
        }
    }
    
    func calculatePlayableDuration() -> Double {
        guard let player = player?.currentItem,
        player.status == .readyToPlay else {
            return 0
        }
        
        var effectiveTimeRange:CMTimeRange?
        let loadedTimeRanges = player.loadedTimeRanges
        for value in loadedTimeRanges {
            let timeRange = value.timeRangeValue
            
            if CMTimeRangeContainsTime(timeRange,
                                       time: player.currentTime()) {
                effectiveTimeRange = timeRange
                break
            }
        }
     
        if let effectiveTimeRange = effectiveTimeRange,
            CMTimeGetSeconds(CMTimeRangeGetEnd(effectiveTimeRange)) > 0 {
            return CMTimeGetSeconds(CMTimeRangeGetEnd(effectiveTimeRange))
        } else {
            return 0
        }
    }
    
    func calculateSeekableDuration() -> Double {
        let timeRange = playerItemSeekableTimeRange()
        if CMTIME_IS_NUMERIC(timeRange.duration) {
            return CMTimeGetSeconds(timeRange.duration)
        }
        return 0
    }
    
    func addPlayerItemObservers() {
        playerItem?.addObserver(self, forKeyPath: PlayerConstants.statusKeyPath, options: .new, context: nil)
        playerItem?.addObserver(self, forKeyPath: PlayerConstants.playbackBufferEmptyKeyPath, options: .new, context: nil)
        playerItem?.addObserver(self, forKeyPath: PlayerConstants.playbackLikelyToKeepUpKeyPath, options: .new, context: nil)
        playerItem?.addObserver(self, forKeyPath: PlayerConstants.timedMetadata, options:.new, context: nil)
        playerItemObserversSet = true
    }
    
    /// Fixes https://github.com/brentvatne/react-native-video/issues/43
    ///  Crashes caused when trying to remove the observer when there is no observer set
    func removePlayerItemObservers() {
        if playerItemObserversSet {
            playerItem?.removeObserver(self, forKeyPath: PlayerConstants.statusKeyPath)
            playerItem?.removeObserver(self, forKeyPath: PlayerConstants.playbackBufferEmptyKeyPath)
            playerItem?.removeObserver(self, forKeyPath: PlayerConstants.playbackLikelyToKeepUpKeyPath)
            playerItem?.removeObserver(self, forKeyPath: PlayerConstants.timedMetadata)
            playerItemObserversSet = false
        }
    }
}
