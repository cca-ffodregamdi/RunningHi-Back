package com.runninghi.runninghibackv2.common.version;

import com.runninghi.runninghibackv2.common.response.ApiResult;
import com.runninghi.runninghibackv2.config.VersionCheckerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class VersionChecker {

    private final VersionCheckerConfig config;

    @GetMapping("/test/app-review")
    public ResponseEntity<ApiResult<Map<String, Boolean>>> checkVersion(@RequestParam("ver") String version) {

        if (config.getExceptions().contains(version)) {
            return ResponseEntity.ok(ApiResult.success("테스터가 아닙니다.", Map.of("isReviewer", false)));
        }

        String[] versionParts = version.split("\\.");
        String[] currentVersionParts = config.getCurrent().split("\\.");

        StringBuilder targetVersion = new StringBuilder();
        StringBuilder targetCurrentVersion = new StringBuilder();
        int arrayLength = currentVersionParts.length;
        for (int i = 0; i < arrayLength; i++) {
            targetVersion.append(versionParts[i]);
            targetCurrentVersion.append(currentVersionParts[i]);
        }

        if (Integer.parseInt(targetVersion.toString()) > Integer.parseInt(targetCurrentVersion.toString())) {
            return ResponseEntity.ok(ApiResult.success("테스터 확인 성공", Map.of("isReviewer", true)));
        } else {
            return ResponseEntity.ok(ApiResult.success("테스터가 아닙니다.", Map.of("isReviewer", false)));
        }
    }

}