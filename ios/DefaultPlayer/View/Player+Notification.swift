//
//  Player+Notification.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/2/18.
//  Copyright © 2018 Applicaster. All rights reserved.
//

import Foundation
import AVKit

extension Player {
    
    func addNotificationsObserver() {
        let defaultCenter = NotificationCenter.default
        
        defaultCenter.addObserver(self,
                                  selector: #selector(self.applicationWillResignActive(notification:)),
                                  name: UIApplication.willResignActiveNotification,
                                  object: nil)
        
        defaultCenter.addObserver(self,
                                  selector: #selector(self.applicationWillEnterForeground(notification:)),
                                  name: UIApplication.didEnterBackgroundNotification,
                                  object: nil)
        defaultCenter.addObserver(self,
                                  selector: #selector(self.applicationDidEnterBackground(notification:)),
                                  name: UIApplication.willEnterForegroundNotification,
                                  object: nil)
        defaultCenter.addObserver(self,
                                  selector: #selector(self.audioRouteChanged(notification:)),
                                  name: AVAudioSession.routeChangeNotification,
                                  object: nil)
    }
    
    @objc func applicationWillResignActive(notification:Notification) {
        if playInBackground || playWhenInactive || paused || playerItem?.status == .failed {
            return
        }
        player?.pause()
        player?.rate = 0.0
    }
    
    @objc func applicationWillEnterForeground(notification:Notification) {
        applyModifiers()
        if playInBackground {
            playerLayer?.player = player
        }
    }
    
    @objc func applicationDidEnterBackground(notification:Notification) {
        if (playInBackground) {
            // Needed to play sound in background. See https://developer.apple.com/library/ios/qa/qa1668/_index.html
            playerLayer?.player = player
        }
    }
    
    @objc func audioRouteChanged(notification:Notification) {        
        if let reason = notification.userInfo?[AVAudioSessionRouteChangeReasonKey] as? Int,
            reason == AVAudioSession.RouteChangeReason.oldDeviceUnavailable.rawValue {
            onVideoAudioBecomingNoisy?(["target":reactTag ?? NSNull()])
        }
    }
}

