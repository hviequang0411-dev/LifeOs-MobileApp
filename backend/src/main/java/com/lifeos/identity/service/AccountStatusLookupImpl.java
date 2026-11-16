package com.lifeos.identity.service;

import com.lifeos.common.security.AccountStatusLookup;
import com.lifeos.identity.repository.AccountStore;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Cài đặt {@link AccountStatusLookup}: một truy vấn theo khóa chính users. */
@Service
class AccountStatusLookupImpl implements AccountStatusLookup {

    private final AccountStore store;

    AccountStatusLookupImpl(AccountStore store) {
        this.store = store;
    }

    @Override
    public boolean deletionPending(UUID userId) {
        return store.deletionScheduledAt(userId).isPresent();
    }
}
