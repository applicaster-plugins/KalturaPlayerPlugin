//
//  PlayerView+ZPDependablePlayerAdDelegatePluginProtocol.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 7/22/19.
//

import Foundation
import ZappCore

extension PlayerView: DependablePlayerAdDelegatePluginProtocol {
    public func advertisementWillPresented(provider: PlayerDependantPluginProtocol) {
        if let onAdChangedState = onAdChangedState {
            onAdChangedState([DependablePlayerAdDelegatePluginProtocolKeys.playingKey:true])
        }
    }
    
    public func advertisementWillDismissed(provider: PlayerDependantPluginProtocol) {
        if let onAdChangedState = onAdChangedState {
            onAdChangedState([DependablePlayerAdDelegatePluginProtocolKeys.playingKey:false])
        }
    }
    
    public func advertisementFailedToLoad(provider: PlayerDependantPluginProtocol) {
        if let onAdChangedState = onAdChangedState {
            onAdChangedState([DependablePlayerAdDelegatePluginProtocolKeys.playingKey:false])
        }
    }
}

