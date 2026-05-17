#!/bin/bash
# Bulk update all imports in the project to match the new layered structure.

declare -A mappings
mappings["com.raota.global.ai"]="com.raota.infrastructure.ai"
mappings["com.raota.global.auth"]="com.raota.infrastructure.auth"
mappings["com.raota.global.cache"]="com.raota.infrastructure.cache"
mappings["com.raota.global.config"]="com.raota.infrastructure.config"
mappings["com.raota.global.file"]="com.raota.infrastructure.file"
mappings["com.raota.global.log"]="com.raota.infrastructure.log"
mappings["com.raota.global.messaging"]="com.raota.infrastructure.messaging"
mappings["com.raota.global.redis"]="com.raota.infrastructure.redis"
mappings["com.raota.global.common"]="com.raota.presentation.common"

mappings["com.raota.domain.auth.service"]="com.raota.application.auth"
mappings["com.raota.domain.member.service"]="com.raota.application.member"
mappings["com.raota.domain.ramenShop.service"]="com.raota.application.ramenShop"
mappings["com.raota.domain.community.service"]="com.raota.application.community"
mappings["com.raota.domain.retrieval.service"]="com.raota.application.retrieval"
mappings["com.raota.admin.ramenShop.service"]="com.raota.application.admin.ramenShop"

mappings["com.raota.domain.auth.controller"]="com.raota.presentation.api.auth"
mappings["com.raota.domain.member.controller"]="com.raota.presentation.api.member"
mappings["com.raota.domain.ramenShop.controller"]="com.raota.presentation.api.ramenShop"
mappings["com.raota.domain.community.presentation"]="com.raota.presentation.api.community"
mappings["com.raota.admin.ramenShop.controller"]="com.raota.presentation.admin.ramenShop"

# DTOs
mappings["com.raota.domain.auth.controller.response"]="com.raota.presentation.api.auth.dto"
mappings["com.raota.domain.member.controller.request"]="com.raota.presentation.api.member.dto"
mappings["com.raota.domain.member.controller.response"]="com.raota.presentation.api.member.dto"
mappings["com.raota.domain.member.dto"]="com.raota.presentation.api.member.dto"
mappings["com.raota.domain.ramenShop.controller.request"]="com.raota.presentation.api.ramenShop.dto"
mappings["com.raota.domain.ramenShop.controller.response"]="com.raota.presentation.api.ramenShop.dto"
mappings["com.raota.domain.ramenShop.dto"]="com.raota.presentation.api.ramenShop.dto"
mappings["com.raota.domain.community.presentation.request"]="com.raota.presentation.api.community.dto"
mappings["com.raota.domain.community.presentation.response"]="com.raota.presentation.api.community.dto"
mappings["com.raota.admin.ramenShop.request"]="com.raota.presentation.admin.ramenShop.dto"
mappings["com.raota.admin.ramenShop.response"]="com.raota.presentation.admin.ramenShop.dto"

# Persistence implementations
mappings["com.raota.domain.auth.store.JpaRefreshTokenStore"]="com.raota.infrastructure.persistence.auth.JpaRefreshTokenStore"
mappings["com.raota.domain.auth.store.RedisRefreshTokenStore"]="com.raota.infrastructure.persistence.auth.RedisRefreshTokenStore"
mappings["com.raota.domain.retrieval.messaging"]="com.raota.infrastructure.messaging.redis"
mappings["com.raota.domain.retrieval.event.PostIndexingEventDispatcher"]="com.raota.infrastructure.messaging.redis.PostIndexingEventDispatcher"

# Contracts
mappings["com.raota.domain.member.controller.contract"]="com.raota.presentation.api.member.contract"
mappings["com.raota.domain.ramenShop.controller.contract"]="com.raota.presentation.api.ramenShop.contract"
mappings["com.raota.domain.community.presentation.contract"]="com.raota.presentation.api.community.contract"

# File
mappings["com.raota.infrastructure.file.FIleController"]="com.raota.presentation.api.file.FIleController"
mappings["com.raota.infrastructure.file.PresignedUrlResponse"]="com.raota.presentation.api.file.dto.PresignedUrlResponse"

# Sorting keys by length descending manually for simple bash compatibility
keys=(
    "com.raota.domain.auth.controller.response"
    "com.raota.domain.member.controller.request"
    "com.raota.domain.member.controller.response"
    "com.raota.domain.ramenShop.controller.request"
    "com.raota.domain.ramenShop.controller.response"
    "com.raota.domain.community.presentation.request"
    "com.raota.domain.community.presentation.response"
    "com.raota.domain.retrieval.event.PostIndexingEventDispatcher"
    "com.raota.domain.member.controller.contract"
    "com.raota.domain.ramenShop.controller.contract"
    "com.raota.domain.community.presentation.contract"
    "com.raota.domain.auth.store.JpaRefreshTokenStore"
    "com.raota.domain.auth.store.RedisRefreshTokenStore"
    "com.raota.infrastructure.file.FIleController"
    "com.raota.infrastructure.file.PresignedUrlResponse"
    "com.raota.domain.auth.service"
    "com.raota.domain.member.service"
    "com.raota.domain.ramenShop.service"
    "com.raota.domain.community.service"
    "com.raota.domain.retrieval.service"
    "com.raota.admin.ramenShop.service"
    "com.raota.domain.auth.controller"
    "com.raota.domain.member.controller"
    "com.raota.domain.ramenShop.controller"
    "com.raota.domain.community.presentation"
    "com.raota.admin.ramenShop.controller"
    "com.raota.admin.ramenShop.request"
    "com.raota.admin.ramenShop.response"
    "com.raota.domain.member.dto"
    "com.raota.domain.ramenShop.dto"
    "com.raota.domain.retrieval.messaging"
    "com.raota.global.ai"
    "com.raota.global.auth"
    "com.raota.global.cache"
    "com.raota.global.config"
    "com.raota.global.file"
    "com.raota.global.log"
    "com.raota.global.messaging"
    "com.raota.global.redis"
    "com.raota.global.common"
)

for key in "${keys[@]}"; do
    val=${mappings[$key]}
    echo "Replacing $key with $val..."
    find src -name "*.java" -exec sed -i '' "s/$key/$val/g" {} +
done
