//
//  PlayerModule.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/2/18.
//  Copyright © 2018 Applicaster. All rights reserved.
//

import Foundation
import React
import AVFoundation

//import ZappPlugins

@objc(PlayerModule)
public class PlayerModule: RCTViewManager {
    static let playerModuleName = "PlayerModule"
    
    override public static func moduleName() -> String? {
        return PlayerModule.playerModuleName
    }
    
    override public class func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    override open var methodQueue: DispatchQueue {
        return bridge.uiManager.methodQueue
    }
    
    override public func view() -> UIView? {
        guard let eventDispatcher = bridge?.eventDispatcher() else {
            return nil
        }
        return PlayerView(eventDispatcher: eventDispatcher)
    }
    
    @objc public override func constantsToExport() -> [AnyHashable : Any]! {
        return [
            "ScaleNone": AVLayerVideoGravity.resizeAspect as Any,
            "ScaleToFill": AVLayerVideoGravity.resize as Any,
            "ScaleAspectFit": AVLayerVideoGravity.resizeAspect as Any,
            "ScaleAspectFill": AVLayerVideoGravity.resizeAspectFill as Any
        ]
    }    
}

