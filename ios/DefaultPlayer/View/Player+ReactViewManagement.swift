//
//  PlayerView+ReactViewManagement.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/6/18.
//

import Foundation
import React.RCTLog
import ZappCore

extension PlayerView {
    public override func insertReactSubview(_ subview: UIView?, at atIndex: Int) {
        // We are early in the game and somebody wants to set a subview.
        // That can only be in the context of playerViewController.
        if controls == false,
            playerLayer == nil,
            playerViewController == nil {
            controls = true
        }

        if let subview = subview,
            self.controls == true {
            subview.frame = self.bounds
            self.playerViewController?.contentOverlayView?.insertSubview(subview,
                                                                         at: atIndex)
        }
    }

    public override func removeReactSubview(_ subview: UIView?) {
        if controls {
            subview?.removeFromSuperview()
        }
    }

    public override func layoutSubviews() {
        super.layoutSubviews()

        // React bug reactonly resize view by width, if find better solution remove it.
        if let superBounds = superview?.bounds {
            frame = superBounds
        }

        if controls,
            let playerViewController = playerViewController,
            let subviews = playerViewController.contentOverlayView?.subviews {
            playerViewController.view.frame = bounds

            // also adjust all subviews of contentOverlayView
            for subview in subviews {
                subview.frame = bounds
            }
        } else {
            CATransaction.begin()
            CATransaction.setAnimationDuration(0)
            playerLayer?.frame = bounds
            CATransaction.commit()
        }
    }

    public override func removeFromSuperview() {
        FacadeConnector.connector?.playerDependant?.playerDidDismiss(player: self)
        clearPlayerData()
        super.removeFromSuperview()
    }
}
