//
//  PlayerView+PlayerDelegate.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/6/18.
//

import Foundation
import AVKit

extension PlayerView: PlayerDelegate {
    
    public func playerViewControllerWillDisappear(playerViewController:AVPlayerViewController) {
        if self.playerViewController == playerViewController,
            fullscreenPlayerPresented == true,
            let onVideoFullscreenPlayerWillDismiss = onVideoFullscreenPlayerWillDismiss {
            onVideoFullscreenPlayerWillDismiss(["target" : reactTag ?? NSNull()])
        }
    }
    
    public func playerViewControllerDidDisappear(playerViewController:AVPlayerViewController) {
        if self.playerViewController == playerViewController,
            fullscreenPlayerPresented == true {
            fullscreenPlayerPresented = false
            presentingViewController = nil
            applyModifiers()
            self.playerViewController = nil

            if let onVideoFullscreenPlayerDidDismiss = onVideoFullscreenPlayerDidDismiss {
                onVideoFullscreenPlayerDidDismiss(["target":reactTag ?? NSNull()])
            }
        }
    }
}
