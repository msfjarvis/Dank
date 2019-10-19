package me.saket.dank.ui.user;

import android.content.Context;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import com.f2prateek.rx.preferences2.Preference;
import com.f2prateek.rx.preferences2.RxSharedPreferences;

import net.dean.jraw.oauth.AccountHelper;

import javax.inject.Inject;
import javax.inject.Named;

import dagger.Lazy;
import io.reactivex.Completable;
import io.reactivex.Observable;
import me.saket.dank.reddit.Reddit;
import me.saket.dank.ui.accountmanager.AccountManager;
import me.saket.dank.ui.accountmanager.AccountManagerRepository;
import me.saket.dank.utils.Optional;
import me.saket.dank.utils.Preconditions;
import timber.log.Timber;

/**
 * TODO: Merge with {@link UserProfileRepository}.
 */
public class UserSessionRepository {

  @Inject AccountHelper accountHelper;
  private static final String KEY_LOGGED_IN_USERNAME = "logged_in_username_v0.8.1";
  private static final String EMPTY = "";

  private Lazy<Reddit> reddit;
  private final Preference<String> loggedInUsername;
  @Inject Lazy<AccountManagerRepository> userManagementRepository;

  @Inject
  public UserSessionRepository(Lazy<Reddit> reddit, @Named("user_session") RxSharedPreferences rxSharedPreferences) {
    this.reddit = reddit;
    loggedInUsername = rxSharedPreferences.getString(KEY_LOGGED_IN_USERNAME, EMPTY);
  }

  public void setLoggedInUsername(String username) {
    Preconditions.checkNotNull(username, "username == null");
    // add user to repository
    this.userManagementRepository.get()
        .add(AccountManager.create(username))
        .subscribe();

    loggedInUsername.set(username);
  }

  public Completable logout() {
    return reddit.get()
        .loggedInUser()
        .logout()
        .andThen(Completable.fromAction(() -> removeLoggedInUsername()));
  }

  public void removeLoggedInUsername() {
    loggedInUsername.set(EMPTY);
  }

  public Completable switchAccount(String username, Context ctx) {
    try {
      if (username == null) {
        accountHelper.switchToUserless();
        loggedInUsername.set(EMPTY);
      } else if (username.equals(loggedInUserName())) {
        Timber.d("Currently logged as this user");
      } else {
        accountHelper.trySwitchToUser(username);
        loggedInUsername.set(username);
      }
    } catch (Exception e) {
      Timber.e(e, "Error while switching users");
    }

    return Completable.complete();
  }

  public boolean isUserLoggedIn() {
    //noinspection ConstantConditions
    return loggedInUserName() != null && !loggedInUserName().equals(EMPTY);
  }

  @Nullable
  public String loggedInUserName() {
    return loggedInUsername.get();
  }

  /** Note: emits the current value immediately. */
  @CheckResult
  public Observable<Optional<UserSession>> streamSessions() {
    return loggedInUsername.asObservable()
        .map(username -> username.equals(EMPTY)
            ? Optional.empty()
            : Optional.of(UserSession.create(username))
        );
  }
}
