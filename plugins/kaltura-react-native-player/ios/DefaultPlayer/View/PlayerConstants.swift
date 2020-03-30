//
//  PlayerConstants.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/2/18.
//  Copyright © 2018 Applicaster. All rights reserved.
//

import Foundation

public struct PlayerConstants {
    static let statusKeyPath                 = "status"
    static let playbackLikelyToKeepUpKeyPath = "playbackLikelyToKeepUp"
    static let playbackBufferEmptyKeyPath    = "playbackBufferEmpty"
    static let readyForDisplayKeyPath        = "readyForDisplay"
    static let playbackRate                  = "rate"
    static let timedMetadata                 = "timedMetadata"
    static let externalPlaybackActive        = "externalPlaybackActive"
}

public struct ReactPropsKey {
    static let src = "src"
    public struct SRC {
        static let url            = "uri"
        static let isNetwork      = "isNetwork"
        static let isAsset        = "isAsset"
        static let type           = "type"
        static let mainVer        = "mainVer"
        static let patchVer       = "patchVer"
        /// Not Supported
        static let requestHeaders = "requestHeaders"
        static let target         = "target"
    }
}

