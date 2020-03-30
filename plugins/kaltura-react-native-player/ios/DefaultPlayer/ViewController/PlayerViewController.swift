//
//  PlayerViewController.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/2/18.
//  Copyright © 2018 Applicaster. All rights reserved.
//

import UIKit
import AVKit

class PlayerViewController: AVPlayerViewController {
    public var playerDelegate:PlayerDelegate?
    
    override func viewDidLoad() {
        super.viewDidLoad()
         #if os(iOS)
             allowsPictureInPicturePlayback = false
         #endif
     }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        playerDelegate?.playerViewControllerWillDisappear(playerViewController: self)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        playerDelegate?.playerViewControllerDidDisappear(playerViewController: self)
    }
}
