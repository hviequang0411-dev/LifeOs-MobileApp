import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../features/auth/data/online_account_service.dart';
import '../../features/notification/data/local_notification_service.dart';
import '../../features/workspace/data/lifeos_local_repository.dart';
import '../config/app_flavor.dart';
import '../database/app_database.dart';
import '../network/api_client.dart';
import '../sync/entity_types.dart';
import '../sync/sync_engine.dart';

final Provider<AppFlavorConfig> appFlavorProvider = Provider<AppFlavorConfig>(
  (Ref ref) => AppFlavorConfig.fromEnvironment(),
);

final Provider<AppDatabase> appDatabaseProvider = Provider<AppDatabase>((Ref ref) {
  final AppDatabase db = AppDatabase.inMemory();
  ref.onDispose(db.close);
  return db;
});

final Provider<TokenStorage> tokenStorageProvider = Provider<TokenStorage>(
  (Ref ref) => InMemoryTokenStorage(),
);

final StateProvider<String> currentUserIdProvider = StateProvider<String>(
  (Ref ref) => 'user-local-01',
);

/// Thời điểm UTC tham chiếu cho giao diện (mặc định `DateTime.now().toUtc()`, có thể ghi đè cố định trong Golden Test).
final Provider<DateTime> referenceUtcProvider = Provider<DateTime>(
  (Ref ref) => DateTime.now().toUtc(),
);

final StateProvider<ThemeMode> themeModeProvider = StateProvider<ThemeMode>(
  (Ref ref) => ThemeMode.light,
);

final StateProvider<bool> privacyMaskAmountsProvider = StateProvider<bool>(
  (Ref ref) => false,
);

final StateProvider<bool> accountDeletionPendingBannerProvider =
    StateProvider<bool>((Ref ref) => false);

final StateProvider<bool> requiresReAuthBannerProvider =
    StateProvider<bool>((Ref ref) => false);

final StateProvider<bool> isSignedOutProvider =
    StateProvider<bool>((Ref ref) => false);

final StateProvider<String> userDisplayNameProvider =
    StateProvider<String>((Ref ref) => 'bạn');

final StateProvider<NotificationPreferences> notificationPreferencesProvider =
    StateProvider<NotificationPreferences>(
  (Ref ref) => const NotificationPreferences(),
);

final StateProvider<NotificationRouteTarget?> pendingNotificationRouteProvider =
    StateProvider<NotificationRouteTarget?>((Ref ref) => null);

final Provider<LocalNotificationGateway> notificationGatewayProvider =
    Provider<LocalNotificationGateway>(
  (Ref ref) => FlutterLocalNotificationsGateway(),
);

final Provider<LocalNotificationCoordinator>
    localNotificationCoordinatorProvider =
    Provider<LocalNotificationCoordinator>((Ref ref) {
  final AppDatabase db = ref.watch(appDatabaseProvider);
  final LocalNotificationGateway gateway =
      ref.watch(notificationGatewayProvider);
  return LocalNotificationCoordinator(
    db: db,
    scheduler: LocalNotificationScheduler(db: db),
    gateway: gateway,
    onRouteRequested: (NotificationRouteTarget target) {
      ref.read(pendingNotificationRouteProvider.notifier).state = target;
    },
  );
});

final Provider<FcmPushCoordinator> fcmPushCoordinatorProvider =
    Provider<FcmPushCoordinator>((Ref ref) {
  final AppFlavorConfig flavor = ref.watch(appFlavorProvider);
  return FcmPushCoordinator(enabled: flavor.enableFcm);
});

final Provider<LifeOsApiClient> apiClientProvider =
    Provider<LifeOsApiClient>((Ref ref) {
  final AppFlavorConfig flavor = ref.watch(appFlavorProvider);
  final TokenStorage storage = ref.watch(tokenStorageProvider);
  return LifeOsApiClient(
    baseUrl: flavor.apiBaseUrl,
    tokenStorage: storage,
    onRequiresReAuth: () {
      ref.read(requiresReAuthBannerProvider.notifier).state = true;
    },
    onAccountDeletionPending: () {
      ref.read(accountDeletionPendingBannerProvider.notifier).state = true;
    },
  );
});

final Provider<LifeOsLocalRepository> localRepositoryProvider =
    Provider<LifeOsLocalRepository>((Ref ref) {
  final AppDatabase db = ref.watch(appDatabaseProvider);
  return LifeOsLocalRepository(db: db);
});

final Provider<SyncEngine> syncEngineProvider = Provider<SyncEngine>((Ref ref) {
  final AppDatabase db = ref.watch(appDatabaseProvider);
  final LifeOsApiClient api = ref.watch(apiClientProvider);
  return SyncEngine(db: db, apiClient: api);
});

final Provider<OnlineAccountService> onlineAccountServiceProvider =
    Provider<OnlineAccountService>((Ref ref) {
  return OnlineAccountService(apiClient: ref.watch(apiClientProvider));
});

// ===========================================================================
// STREAM PROVIDERS ĐỌC TRỰC TIẾP TỪ SQLITE CỤC BỘ (OFFLINE-FIRST)
// ===========================================================================

final StreamProvider<LocalSyncStateRecord> syncStateStreamProvider =
    StreamProvider<LocalSyncStateRecord>((Ref ref) {
  final AppDatabase db = ref.watch(appDatabaseProvider);
  final String userId = ref.watch(currentUserIdProvider);
  return db.watchSyncState(userId);
});

final StreamProvider<List<OutboxMutationRecord>> outboxStreamProvider =
    StreamProvider<List<OutboxMutationRecord>>((Ref ref) {
  final AppDatabase db = ref.watch(appDatabaseProvider);
  final String userId = ref.watch(currentUserIdProvider);
  return db.watchOutboxMutations(userId);
});

final StreamProviderFamily<List<LocalEntityRow>, SyncEntityType>
    entitiesByTypeStreamProvider =
    StreamProvider.family<List<LocalEntityRow>, SyncEntityType>(
        (Ref ref, SyncEntityType type) {
  final AppDatabase db = ref.watch(appDatabaseProvider);
  final String userId = ref.watch(currentUserIdProvider);
  return db.watchEntities(userId: userId, entityType: type);
});

