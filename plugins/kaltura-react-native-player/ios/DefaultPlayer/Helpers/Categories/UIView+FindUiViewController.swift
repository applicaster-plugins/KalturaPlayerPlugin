//
//  UIView+FindUiViewController.swift
//  DefaultPlayer
//
//  Created by Anton Kononenko on 12/6/18.
//

//  Source: http://stackoverflow.com/a/3732812/1123156

extension UIView {
    func firstAvailableUIViewController() -> UIViewController? {
        return traverseResponderChainForUIViewController()
    }
    
    func traverseResponderChainForUIViewController() -> UIViewController? {
        if let nextResponder = next as? UIViewController {
            return nextResponder
        } else if let nextResponder = next as? UIView  {
            return nextResponder.traverseResponderChainForUIViewController()
        } else {
            return nil
        }
    }
}
