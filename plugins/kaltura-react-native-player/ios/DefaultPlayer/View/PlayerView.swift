//
//  Player.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/2/18.
//  Copyright © 2018 Applicaster. All rights reserved.
//

import AVKit
import Foundation
import React
import ZappCore

@objc public class PlayerView: UIView {
    @objc public var onVideoLoadStart: RCTBubblingEventBlock?
    @objc public var onVideoLoad: RCTBubblingEventBlock?
    @objc public var onVideoBuffer: RCTBubblingEventBlock?
    @objc public var onVideoError: RCTBubblingEventBlock?
    @objc public var onVideoProgress: RCTBubblingEventBlock?
    @objc public var onVideoSeek: RCTBubblingEventBlock?
    @objc public var onVideoEnd: RCTBubblingEventBlock?
    @objc public var onTimedMetadata: RCTBubblingEventBlock?
    @objc public var onVideoAudioBecomingNoisy: RCTBubblingEventBlock?
    @objc public var onVideoFullscreenPlayerWillPresent: RCTBubblingEventBlock?
    @objc public var onVideoFullscreenPlayerDidPresent: RCTBubblingEventBlock?
    @objc public var onVideoFullscreenPlayerWillDismiss: RCTBubblingEventBlock?
    @objc public var onVideoFullscreenPlayerDidDismiss: RCTBubblingEventBlock?
    @objc public var onReadyForDisplay: RCTBubblingEventBlock?
    @objc public var onPlaybackStalled: RCTBubblingEventBlock?
    @objc public var onPlaybackResume: RCTBubblingEventBlock?
    @objc public var onPlaybackRateChange: RCTBubblingEventBlock?
    @objc public var onVideoExternalPlaybackChange: RCTBubblingEventBlock?
    @objc public var onAdChangedState: RCTBubblingEventBlock?

    @objc public var entry: [String: Any]?

    @objc public var src: NSDictionary? {
        didSet {
            removePlayerLayer()
            removePlayerTimeObserver()
            removePlayerItemObservers()

            // Perform on next run loop, otherwise other passed react-props may not be set
            DispatchQueue.main.asyncAfter(deadline: .now() + 1) { [weak self] in

                self?.playerItemForSource(source: self?.src as? [String: Any],
                                          completion: { playerItem in
                                              guard let playerItem = playerItem else {
                                                  return
                                              }
                                              self?.preparePlayerForUsage(source: self?.src,
                                                                          playerItem: playerItem)
                })
            }
            videoLoadStarted = true
        }
    }

    func preparePlayerForUsage(source: NSDictionary?,
                               playerItem: AVPlayerItem) {
        self.playerItem = playerItem

        addPlayerItemObservers()
        if let applyFilter = filter {
            filter = applyFilter
        }
        player?.pause()
        removePlayerViewController()

        if playbackRateObserverRegistered == true {
            player?.removeObserver(self,
                                   forKeyPath: PlayerConstants.playbackRate,
                                   context: nil)
            playbackRateObserverRegistered = false
        }

        if isExternalPlaybackActiveObserverRegistered == true {
            player?.removeObserver(self,
                                   forKeyPath: PlayerConstants.externalPlaybackActive,
                                   context: nil)
            isExternalPlaybackActiveObserverRegistered = false
        }

        player = AVPlayer(playerItem: playerItem)
        player?.actionAtItemEnd = .none

        player?.addObserver(self,
                            forKeyPath: PlayerConstants.playbackRate,
                            options: [],
                            context: nil)
        playbackRateObserverRegistered = true

        player?.addObserver(self,
                            forKeyPath: PlayerConstants.externalPlaybackActive,
                            options: [],
                            context: nil)
        isExternalPlaybackActiveObserverRegistered = true
        addPlayerTimeObserver()

        // Perform on next run loop, otherwise onVideoLoadStart is nil
        if let onVideoLoadStart = onVideoLoadStart {
            let data = [ReactPropsKey.SRC.url: source?[ReactPropsKey.SRC.url] ?? NSNull(),
                        ReactPropsKey.SRC.type: source?[ReactPropsKey.SRC.type] ?? NSNull(),
                        ReactPropsKey.SRC.isNetwork: source?[ReactPropsKey.SRC.isNetwork] ?? false,
                        ReactPropsKey.SRC.isAsset: source?[ReactPropsKey.SRC.isAsset] ?? false,
                        ReactPropsKey.SRC.target: reactTag ?? NSNull()]
            onVideoLoadStart(["src": data])
        }

        videoLoadStarted = true
    }

    var playerViewController: PlayerViewController?
    public var player: AVPlayer?
    var playerItem: AVPlayerItem?
    var playerLayer: AVPlayerLayer?
    var isPlayerLayerObserverSet: Bool = false

    var videoURL: URL?

    /* Required to publish events */
    var eventDispatcher: RCTEventDispatcher?

    var playerItemObserversSet = false
    var playerBufferEmpty = true
    var playbackRateObserverRegistered = false
    var isExternalPlaybackActiveObserverRegistered = false
    var videoLoadStarted = false
    var pendingSeek = false
    var pendingSeekTime: Float = 0.0
    var lastSeekTime: Float = 0.0

    /* For sending videoProgress events */
    @objc public var progressUpdateInterval: Float64 = 250 {
        didSet {
            if timeObserver != nil {
                removePlayerTimeObserver()
                addPlayerTimeObserver()
            }
        }
    }

    var _controls: Bool = true
    @objc public var controls: Bool {
        set {
            if _controls != newValue || (playerLayer == nil && playerViewController == nil) {
                _controls = newValue
                if _controls {
                    removePlayerLayer()
                    usePlayerViewController()
                } else {
                    removePlayerViewController()
                    usePlayerLayer()
                }
            }
        }
        get {
            return _controls
        }
    }

    var timeObserver: Any?

    @objc public var currentTime: Float {
        get {
            if let playerItem = playerItem {
                return Float(CMTimeGetSeconds(playerItem.currentTime()))
            } else {
                return 0
            }
        }
        set {
            seek = ["time": newValue,
                    "tolerance": 100]
        }
    }

    @objc public var seek: NSDictionary? {
        get {
            return nil
        }
        set {
            guard let seekTime = newValue?["time"] as? Float64 else {
                return
            }
            let timeScale: Int32 = 1000

            if let seekTimeTolerance = newValue?["tolerance"] as? Int64,
                let item = player?.currentItem,
                item.status == .readyToPlay {
                let cmSeekTime = CMTimeMakeWithSeconds(seekTime,
                                                       preferredTimescale: timeScale)
                let current = item.currentTime()
                let tolerance = CMTimeMake(value: seekTimeTolerance,
                                           timescale: timeScale)

                let isPaused = paused

                if CMTimeCompare(current, cmSeekTime) != 0 {
                    if isPaused == false {
                        player?.pause()
                    }
                    player?.seek(to: cmSeekTime,
                                 toleranceBefore: tolerance,
                                 toleranceAfter: tolerance,
                                 completionHandler: { [weak self] _ in
                                     if self?.timeObserver == nil {
                                         self?.addPlayerTimeObserver()
                                     }
                                     if isPaused == false {
                                         self?.paused = false
                                     }

                                     if let onVideoSeek = self?.onVideoSeek,
                                         let reactTag = self?.reactTag {
                                         onVideoSeek(["currentTime": item.currentTime,
                                                      "seekTime": seekTime,
                                                      "target": reactTag])
                                     }
                    })
                    pendingSeek = false
                }
            } else {
                pendingSeek = true
                pendingSeekTime = Float(seekTime)
            }
        }
    }

    /* Keep track of any modifiers, need to be applied after each play */
    @objc public var volume: Float = 1.0 {
        didSet {
            applyModifiers()
        }
    }

    @objc public var rate: Float = 1.0 {
        didSet {
            applyModifiers()
        }
    }

    @objc public var muted: Bool = false {
        didSet {
            applyModifiers()
        }
    }

    @objc public var paused: Bool = false {
        didSet {
            if paused {
                player?.pause()
                player?.rate = 0.0

            } else {
                if ignoreSilentSwitch == "ignore" {
                    do {
                        try AVAudioSession.sharedInstance().setCategory(AVAudioSession.Category.playback,
                                                                        mode: .default,
                                                                        options: .duckOthers)
                    } catch {
                        print(error)
                    }
                } else if ignoreSilentSwitch == "obey" {
                    do {
                        try AVAudioSession.sharedInstance().setCategory(AVAudioSession.Category.ambient,
                                                                        mode: .default,
                                                                        options: .duckOthers)
                    } catch {
                        print(error)
                    }
                }
                player?.play()
                player?.rate = rate
            }
        }
    }

    @objc public var repeatVideo: Bool = false
    @objc public var allowsExternalPlayback: Bool = true {
        didSet {
            player?.allowsExternalPlayback = allowsExternalPlayback
        }
    }

    @objc public var playbackStalled: Bool = false
    @objc public var playInBackground: Bool = false
    @objc public var playWhenInactive: Bool = false
    @objc public var ignoreSilentSwitch: String = "inherit" { // inherit, ignore, obey Х
        didSet {
            applyModifiers()
        }
    }

    @objc public var resizeMode: String = "AVLayerVideoGravityResizeAspectFill" {
        didSet {
            if controls {
                playerViewController?.videoGravity = AVLayerVideoGravity(rawValue: resizeMode)
            } else {
                playerLayer?.videoGravity = AVLayerVideoGravity(rawValue: resizeMode)
            }
        }
    }

    var fullscreenPlayerPresented: Bool = false
    @objc public var fullScreen: Bool = false

    public func tryPresentFullScreen() {
        guard let playerViewController = playerViewController else {
            return
        }
        if fullScreen == true,
            fullscreenPlayerPresented == false,
            player != nil {
            // Set presentation style to fullscreen
            playerViewController.modalPresentationStyle = .fullScreen

            // Find the nearest view controller
            var viewController = firstAvailableUIViewController()
            if viewController == nil,
                let keyWindow = UIApplication.shared.keyWindow {
                viewController = keyWindow.rootViewController
                if let children = viewController?.children {
                    viewController = children.last
                }
            }

            if let viewController = viewController {
                presentingViewController = viewController
                if let onVideoFullscreenPlayerWillPresent = onVideoFullscreenPlayerWillPresent {
                    onVideoFullscreenPlayerWillPresent(["target": reactTag ?? NSNull()])
                }
                viewController.present(playerViewController, animated: false) { [weak self] in
                    playerViewController.showsPlaybackControls = true
                    self?.fullscreenPlayerPresented = self?.fullScreen ?? true
                    if let onVideoFullscreenPlayerDidPresent = self?.onVideoFullscreenPlayerDidPresent,
                        let reactTag = self?.reactTag {
                        onVideoFullscreenPlayerDidPresent(["target": reactTag])
                    }
                }
            }

        } else if fullScreen == false,
            fullscreenPlayerPresented == true {
            playerViewControllerWillDisappear(playerViewController: playerViewController)
            presentingViewController?.dismiss(animated: false, completion: { [weak self] in
                self?.playerViewControllerDidDisappear(playerViewController: playerViewController)
            })
        }
    }

    @objc public var filter: String? {
        didSet {
            guard let filter = filter,
                let asset = playerItem?.asset else {
                return
            }
            let filterItem = CIFilter(name: filter)
            playerItem?.videoComposition = AVVideoComposition(asset: asset,
                                                              applyingCIFiltersWithHandler: { request in
                                                                  if let filterItem = filterItem {
                                                                      let image = request.sourceImage.clampedToExtent()
                                                                      filterItem.setValue(image,
                                                                                          forKey: kCIInputImageKey)
                                                                      if let image = filterItem.outputImage {
                                                                          let output = image.cropped(to: request.sourceImage.extent)
                                                                          request.finish(with: output,
                                                                                         context: nil)
                                                                      }
                                                                  } else {
                                                                      request.finish(with: request.sourceImage,
                                                                                     context: nil)
                                                                  }

            })
        }
    }

    @objc public var presentingViewController: UIViewController?

    public init(eventDispatcher: RCTEventDispatcher) {
        super.init(frame: .zero)
        addNotificationsObserver()
    }

    public required init?(coder aDecoder: NSCoder) {
        return nil
    }

    deinit {
        clearPlayerData()
        FacadeConnector.connector?.playerDependant?.playerDidDismiss(player: self)
    }

    func removePlayerLayer() {
        if playerLayer != nil {
            playerLayer?.removeFromSuperlayer()
            if isPlayerLayerObserverSet {
                playerLayer?.removeObserver(self,
                                            forKeyPath: PlayerConstants.readyForDisplayKeyPath)
                isPlayerLayerObserverSet = false
            }
            playerLayer = nil
        }
    }

    func createPlayerViewController(player: AVPlayer, playerItem: AVPlayerItem) -> PlayerViewController {
        let playerViewController = PlayerViewController()
        playerViewController.showsPlaybackControls = true
        playerViewController.playerDelegate = self
        playerViewController.view.frame = bounds
        playerViewController.player = player
        playerViewController.view.frame = bounds
        return playerViewController
    }

    func applyModifiers() {
        guard let playerItem = playerItem,
            playerItem.status != .failed else {
            return
        }
        if muted {
            player?.volume = 0
            player?.isMuted = true
        } else {
            player?.volume = volume
            player?.isMuted = false
        }

        resizeMode = String(resizeMode)
        repeatVideo = Bool(repeatVideo)
        paused = Bool(paused)
        controls = Bool(controls)
        allowsExternalPlayback = Bool(allowsExternalPlayback)
    }

    func usePlayerViewController() {
        guard let player = player,
            let playerItem = playerItem
        else {
            return
        }

        playerViewController = createPlayerViewController(player: player,
                                                          playerItem: playerItem)
        // to prevent video from being animated when resizeMode is 'cover'
        // resize mode must be set before subview is added
        resizeMode = String(resizeMode)
        if fullScreen {
            tryPresentFullScreen()
        } else {
            if let playerViewController = playerViewController {
                addSubview(playerViewController.view)
            }
        }

        FacadeConnector.connector?.playerDependant?.playerDidCreate(player: self)
    }

    func usePlayerLayer() {
        guard let player = player
        else {
            return
        }

        playerLayer = AVPlayerLayer(player: player)
        if let playerLayer = playerLayer {
            playerLayer.frame = bounds
            playerLayer.needsDisplayOnBoundsChange = true

            // to prevent video from being animated when resizeMode is 'cover'
            // resize mode must be set before layer is added
            resizeMode = String(resizeMode)
            playerLayer.addObserver(self,
                                    forKeyPath: PlayerConstants.readyForDisplayKeyPath,
                                    options: .new,
                                    context: nil)
            isPlayerLayerObserverSet = true

            layer.addSublayer(playerLayer)
            layer.needsDisplayOnBoundsChange = true
        }

        FacadeConnector.connector?.playerDependant?.playerDidCreate(player: self)
    }

    func clearPlayerData() {
        player?.pause()
        if playbackRateObserverRegistered {
            player?.removeObserver(self,
                                   forKeyPath: PlayerConstants.playbackRate,
                                   context: nil)
            playbackRateObserverRegistered = false
        }
        if isExternalPlaybackActiveObserverRegistered {
            player?.removeObserver(self,
                                   forKeyPath: PlayerConstants.externalPlaybackActive,
                                   context: nil)
            isExternalPlaybackActiveObserverRegistered = false
        }
        player = nil
        removePlayerLayer()
        removePlayerViewController()
        removePlayerTimeObserver()
        removePlayerItemObservers()

        eventDispatcher = nil
        NotificationCenter.default.removeObserver(self)
    }

    func removePlayerViewController() {
        if playerViewController != nil {
            playerViewController?.view.removeFromSuperview()
            if playerViewController?.presentingViewController != nil {
                playerViewController?.dismiss(animated: false,
                                              completion: nil)
            }
            playerViewController = nil
        }
    }
}
