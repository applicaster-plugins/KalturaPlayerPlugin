//
//  PlayerDelegate.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/2/18.
//  Copyright © 2018 Applicaster. All rights reserved.
//

import Foundation
import AVKit

public protocol PlayerDelegate {
    
    /// Player Controller will be dismissed
    ///
    /// - Parameter playerViewController: Instance of the player view controller
    func playerViewControllerWillDisappear(playerViewController:AVPlayerViewController)
    
    /// Player Controller did  dismissed
    ///
    /// - Parameter playerViewController: Instance of the player view controller
    func playerViewControllerDidDisappear(playerViewController:AVPlayerViewController)
}
