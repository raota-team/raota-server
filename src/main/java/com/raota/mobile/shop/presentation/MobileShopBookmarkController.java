package com.raota.mobile.shop.presentation;

import com.raota.mobile.common.presentation.LoginUser;
import com.raota.mobile.shop.application.service.MobileShopBookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 활성 회원의 매장 가고 싶어요를 저장하거나 해제한다. */
@RestController
@RequestMapping("/api/v2/shops")
@RequiredArgsConstructor
public class MobileShopBookmarkController {

    private final MobileShopBookmarkService bookmarks;

    @PutMapping("/{shopId:[0-9]+}/bookmark")
    public ResponseEntity<Void> add(@PathVariable Long shopId, @LoginUser Long userId) {
        bookmarks.add(userId, shopId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{shopId:[0-9]+}/bookmark")
    public ResponseEntity<Void> remove(@PathVariable Long shopId, @LoginUser Long userId) {
        bookmarks.remove(userId, shopId);
        return ResponseEntity.noContent().build();
    }

}
