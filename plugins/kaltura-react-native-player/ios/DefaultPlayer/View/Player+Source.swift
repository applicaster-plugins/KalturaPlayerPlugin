//
//  PlayerView+Source.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/2/18.
//  Copyright © 2018 Applicaster. All rights reserved.
//

import AVKit
import Foundation
import React

extension PlayerView {
    func playerItemForSource(source: [String: Any]?,
                             completion: (_ playerItem: AVPlayerItem?) -> Void) {
        guard let source = source else {
            completion(nil)
            return
        }

        let isNetwork = RCTConvert.bool(source[ReactPropsKey.SRC.isNetwork])
        let isAsset = RCTConvert.bool(source[ReactPropsKey.SRC.isAsset])

        guard let urlString = source[ReactPropsKey.SRC.url] as? String else {
            completion(nil)
            return
        }
        var assetOptions: [String: Any] = [:]

        if isNetwork,
            let url = URL(string: urlString) {
            assetOptions[AVURLAssetHTTPCookiesKey] = HTTPCookieStorage.shared.cookies
            let asset = AVURLAsset(url: url,
                                   options: assetOptions)
            completion(AVPlayerItem(asset: asset))
            return
        } else if isAsset,
            let url = URL(string: urlString) {
            let asset = AVURLAsset(url: url,
                                   options: nil)
            completion(AVPlayerItem(asset: asset))
            return
        }
        // TODO: If needed add suport of custom bundles
        if let type = source[ReactPropsKey.SRC.type] as? String,
            let path = Bundle.main.path(forResource: urlString, ofType: type) {
            let url = URL(fileURLWithPath: path)
            let asset = AVURLAsset(url: url,
                                   options: nil)
            completion(AVPlayerItem(asset: asset))
            return
        } else {
            completion(nil)
        }
    }
}
