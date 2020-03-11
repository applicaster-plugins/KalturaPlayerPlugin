//
//  Player+Observers.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/5/18.
//

import AVFoundation
import Foundation
import ZappCore

extension Player {
    public override func observeValue(forKeyPath keyPath: String?,
                                      of object: Any?,
                                      change: [NSKeyValueChangeKey: Any]?,
                                      context: UnsafeMutableRawPointer?) {
        if let playerItem = playerItem,
            let object = object as? AVPlayerItem,
            object === playerItem {
            // When timeMetadata is read the event onTimedMetadata is triggered
            if keyPath == PlayerConstants.timedMetadata,
                let items = change?[NSKeyValueChangeKey.newKey] as? [AVMetadataItem],
                items.count > 0 {
                var array: [Any] = []
                for item in items {
                    if let value = item.value as? String,
                        let identifier = item.identifier {
                        let dict: [String: Any] = ["value": value,
                                                   "identifier": identifier]
                        array.append(dict)
                    }
                }

                if let onTimedMetadata = onTimedMetadata {
                    let timedMetaData = reactTag ?? NSNull()
                    onTimedMetadata([ReactPropsKey.SRC.target: timedMetaData,
                                     "metadata": array])
                }
            } else if keyPath == PlayerConstants.statusKeyPath {
                // Handle player item status change.
                if playerItem.status == .readyToPlay {
                    var duration = CMTimeGetSeconds(playerItem.asset.duration)
                    if duration.isNaN {
                        duration = 0.0
                    }

                    let undefined = "undefined"
                    var width: CGFloat = 0
                    var height: CGFloat = 0
                    var orientation = undefined

                    if playerItem.asset.tracks(withMediaType: .video).count > 0,
                        let videoTrack = playerItem.asset.tracks(withMediaType: .video).first {
                        width = videoTrack.naturalSize.width
                        height = videoTrack.naturalSize.height
                        let preferredTransform = videoTrack.preferredTransform
                        if (videoTrack.naturalSize.width == preferredTransform.tx
                            && videoTrack.naturalSize.height == preferredTransform.ty)
                            || (preferredTransform.tx == 0 && preferredTransform.ty == 0) {
                            orientation = "landscape"
                        } else {
                            orientation = "portrait"
                        }
                    }

                    if let onVideoLoad = onVideoLoad,
                        videoLoadStarted == true {
                        let sizeDict: [String: Any] = ["width": width,
                                                       "height": height,
                                                       "orientation": orientation]
                        onVideoLoad(["duration": duration,
                                     "currentTime": CMTimeGetSeconds(playerItem.currentTime()),
                                     "canPlayReverse": playerItem.canPlayReverse,
                                     "canPlayFastForward": playerItem.canPlayFastForward,
                                     "canPlaySlowForward": playerItem.canPlaySlowForward,
                                     "canStepBackward": playerItem.canStepBackward,
                                     "canStepForward": playerItem.canStepForward,
                                     "naturalSize": sizeDict,
                                     "target": reactTag ?? NSNull(),
                        ])
                    }
                    videoLoadStarted = false
                    attachListeners()
                    applyModifiers()

                } else if playerItem.status == .failed,
                    let onVideoError = onVideoError,
                    let localizedDescription = playerItem.error?.localizedDescription {
                    let errodDict = ["localizedDescription": localizedDescription]
                    onVideoError(["error": errodDict,
                                  "target": reactTag ?? NSNull(),
                    ])
                }
            } else if keyPath == PlayerConstants.playbackBufferEmptyKeyPath {
                playerBufferEmpty = true
                if let onVideoBuffer = onVideoBuffer {
                    onVideoBuffer(["isBuffering": true,
                                   "target": reactTag ?? NSNull()])
                }
            } else if keyPath == PlayerConstants.playbackLikelyToKeepUpKeyPath {
                // Continue playing (or not if paused) after being paused due to hitting an unbuffered zone.
                if (!(controls || fullscreenPlayerPresented) || playerBufferEmpty) && playerItem.isPlaybackLikelyToKeepUp {
                    let newValue = paused
                    paused = newValue
                }
                playerBufferEmpty = false
                if let onVideoBuffer = onVideoBuffer {
                    onVideoBuffer(["target": reactTag ?? NSNull()])
                }
            }
        } else if let object = object as? AVPlayerLayer,
            let playerLayer = playerLayer,
            object == playerLayer,
            keyPath == PlayerConstants.readyForDisplayKeyPath,
            change?[NSKeyValueChangeKey.newKey] != nil,
            let onReadyForDisplay = onReadyForDisplay {
            onReadyForDisplay(["target": reactTag ?? NSNull()])
        } else if let object = object as? AVPlayer,
            let player = player,
            object == player {
            if keyPath == PlayerConstants.playbackRate {
                if let onPlaybackRateChange = onPlaybackRateChange {
                    onPlaybackRateChange(["playbackRate": player.rate,
                                          "target": reactTag ?? NSNull()])
                }

                if playbackStalled, player.rate > 0 {
                    if let onPlaybackRateChange = onPlaybackRateChange {
                        onPlaybackRateChange(["playbackRate": player.rate,
                                              "target": reactTag ?? NSNull()])
                    }
                    playbackStalled = false
                }
            } else if keyPath == PlayerConstants.externalPlaybackActive,
                let onVideoExternalPlaybackChange = onVideoExternalPlaybackChange {
                onVideoExternalPlaybackChange(["isExternalPlaybackActive": "player.isExternalPlaybackActive",
                                               "target": reactTag ?? NSNull()])
            }
        } else {
            super.observeValue(forKeyPath: keyPath, of: object, change: change, context: context)
        }
    }

    func attachListeners() {
        let notificationCenter = NotificationCenter.default
        notificationCenter.removeObserver(self,
                                          name: NSNotification.Name.AVPlayerItemDidPlayToEndTime,
                                          object: player?.currentItem)
        notificationCenter.addObserver(self,
                                       selector: #selector(playerItemDidReachEnd(notification:)),
                                       name: NSNotification.Name.AVPlayerItemDidPlayToEndTime,
                                       object: player?.currentItem)

        notificationCenter.removeObserver(self,
                                          name: NSNotification.Name.AVPlayerItemPlaybackStalled,
                                          object: nil)
        notificationCenter.addObserver(self,
                                       selector: #selector(playbackStalled(notification:)),
                                       name: NSNotification.Name.AVPlayerItemPlaybackStalled,
                                       object: nil)
    }

    @objc func playbackStalled(notification: Notification) {
        if let onPlaybackStalled = onPlaybackStalled,
            let reactTag = reactTag {
            onPlaybackStalled(["target": reactTag])
        }
        playbackStalled = true
    }

    @objc func playerItemDidReachEnd(notification: Notification) {
        guard let playerDependant = FacadeConnector.connector?.playerDependant else {
            return playerItemDidReachEndHandler(notification: notification)
        }
        playerDependant.playerDidFinishPlayItem(player: self) { [weak self] _ in
            self?.playerItemDidReachEndHandler(notification: notification)
        }
    }

    func playerItemDidReachEndHandler(notification: Notification) {
        if let onVideoEnd = self.onVideoEnd {
            if let reactTag = self.reactTag {
                onVideoEnd(["target": reactTag])
            }
        }

        if repeatVideo == true {
            if let item = notification.object as? AVPlayerItem {
                item.seek(to: .zero,
                          completionHandler: nil)
                applyModifiers()
            }
        } else {
            removePlayerTimeObserver()
        }
    }
}
