package com.africa.dinthialma_backend.auth.service.impl;

import com.africa.dinthialma_backend.auth.dto.UserSearchResponse;
import com.africa.dinthialma_backend.auth.repository.UserRepository;
import com.africa.dinthialma_backend.auth.service.interfaces.UserSearchService;
import com.africa.dinthialma_backend.common.constants.ResponseMessageConstants;
import com.africa.dinthialma_backend.common.exception.CustomException;
import com.africa.dinthialma_backend.common.exception.NotFoundException;
import com.africa.dinthialma_backend.common.util.PhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSearchServiceImpl implements UserSearchService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserSearchResponse searchByPhone(String phone) throws CustomException {
    return userRepository
        .findByPhoneAndDeletedAtIsNull(PhoneUtils.normalize(phone))
        .map(UserSearchResponse::from)
        .orElseThrow(() -> new NotFoundException(ResponseMessageConstants.USER_SEARCH_NOT_FOUND));
  }
}
