package com.balex.rag.service;

import com.balex.rag.model.entity.RefreshToken;
import com.balex.rag.model.entity.User;

public interface RefreshTokenService {

    RefreshToken generateOrUpdateRefreshToken(User user);

    RefreshToken validateAndRefreshToken(String refreshToken);

}
