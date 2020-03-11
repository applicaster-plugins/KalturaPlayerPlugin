//
//  Player+Duration.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/5/18.
//

import Foundation
import CoreMedia

extension Player {
    func playerItemDuration() -> CMTime {
        guard let playerItem = player?.currentItem else {
            return CMTime.invalid
        }
        return playerItem.duration
    }
    
    func playerItemSeekableTimeRange() -> CMTimeRange {
        guard let playerItem = player?.currentItem,
            playerItem.status == .readyToPlay,
            let timeRange = playerItem.seekableTimeRanges.first?.timeRangeValue else {
                return CMTimeRange.zero
        }
        
        return timeRange
    }
    
    func addPlayerTimeObserver() {
        let progressUpdateIntervalMS = progressUpdateInterval / 1000
        timeObserver = player?.addPeriodicTimeObserver(forInterval: CMTimeMakeWithSeconds(progressUpdateIntervalMS,
                                                                                          preferredTimescale: Int32(NSEC_PER_SEC)),
                                                       queue: nil, using: { [weak self] (time) in
                                                        self?.sendProgressUpdate()
        })
    }
    
    /// Cancels the previously registered time observer.
    func removePlayerTimeObserver() {
        if let currentTimeObserver = timeObserver {
            player?.removeTimeObserver(currentTimeObserver)
            timeObserver = nil
        }
    }
}


