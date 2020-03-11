//
//  PlayerView+QBPlayerProtocol.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 7/23/19.
//  Copyright © 2019 Applicaster. All rights reserved.
//

import Foundation
import ZappCore
import CoreMedia

extension PlayerView: PlayerProtocol {
    public func playbackPosition() -> TimeInterval {
        return TimeInterval(currentTime)
    }
    
    public func playbackDuration() -> TimeInterval {
        return TimeInterval(CMTimeGetSeconds(playerItemDuration()))
    }
    
    public func pluggablePlayerPause() {
        player?.pause()
    }
    
    /// Resume playing loaded item
    public func pluggablePlayerResume() {
        player?.play()
    }
    
    public var playerObject:NSObject? {
        return self.player
    }
    
    @objc public var pluginPlayerContainer:UIView? {
        return playerViewController?.view ?? self
    }
    
    @objc public var pluginPlayerViewController:UIViewController? {
        return playerViewController
    }
}
