import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/l10n/vi_strings.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/providers/app_providers.dart';
import '../../../../core/theme/app_tokens.dart';
import '../../../../core/widgets/lifeos_widgets.dart';
import '../../../auth/data/online_account_service.dart';
import '../../../workspace/data/lifeos_local_repository.dart';

/// Sheet cài đặt thông tin cá nhân & tùy chọn vùng (`user_settings` + `PATCH /me`).
class PersonalSettingsSheet extends ConsumerStatefulWidget {
  const PersonalSettingsSheet({super.key});

  @override
  ConsumerState<PersonalSettingsSheet> createState() =>
      _PersonalSettingsSheetState();
}

class _PersonalSettingsSheetState extends ConsumerState<PersonalSettingsSheet> {
  static const List<String> _timezoneOptions = <String>[
    'Asia/Ho_Chi_Minh',
    'UTC',
    'Asia/Tokyo',
    'Asia/Singapore',
    'Europe/London',
    'America/New_York',
  ];

  static const List<String> _currencyOptions = <String>[
    'VND',
    'USD',
    'EUR',
    'JPY',
    'SGD',
  ];

  late final TextEditingController _displayNameCtrl;
  late String _initialDisplayName;
  late String _timezone;
  late String _currency;
  late int _weekStartDay;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    final String userId = ref.read(currentUserIdProvider);
    final Map<String, dynamic> settings =
        ref.read(localRepositoryProvider).getUserSettings(userId);
    _initialDisplayName = ref.read(userDisplayNameProvider);
    _displayNameCtrl = TextEditingController(text: _initialDisplayName);
    _timezone = (settings['timezone'] as String?) ?? 'Asia/Ho_Chi_Minh';
    _currency = ((settings['defaultCurrency'] as String?) ?? 'VND').toUpperCase();
    final int rawWeekStart = (settings['weekStartDay'] as num?)?.toInt() ?? 1;
    _weekStartDay = rawWeekStart == 7 ? 7 : 1;

    WidgetsBinding.instance.addPostFrameCallback((_) => _syncProfileSilently());
  }

  Future<void> _syncProfileSilently() async {
    try {
      final profile = await ref.read(onlineAccountServiceProvider).fetchProfile();
      if (!mounted) return;
      ref.read(userDisplayNameProvider.notifier).state = profile.displayName;
      if (_displayNameCtrl.text.trim() == _initialDisplayName) {
        _displayNameCtrl.text = profile.displayName;
        _initialDisplayName = profile.displayName;
      }
    } catch (_) {
      // Ngoại tuyến hoặc chưa đăng nhập: giữ giá trị đang hiển thị trên máy.
    }
  }

  @override
  void dispose() {
    _displayNameCtrl.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (_saving) return;
    setState(() => _saving = true);

    final String userId = ref.read(currentUserIdProvider);
    final LifeOsLocalRepository repo = ref.read(localRepositoryProvider);
    final DateTime nowUtc = ref.read(referenceUtcProvider);
    final String trimmedName = _displayNameCtrl.text.trim();

    repo.updateUserSettings(
      userId: userId,
      patch: <String, dynamic>{
        'timezone': _timezone,
        'defaultCurrency': _currency,
        'weekStartDay': _weekStartDay,
      },
      nowUtc: nowUtc,
    );

    if (trimmedName.isNotEmpty) {
      ref.read(userDisplayNameProvider.notifier).state = trimmedName;
    }

    String feedback = 'Đã lưu thông tin cá nhân và cài đặt vùng.';
    if (trimmedName.isNotEmpty && trimmedName != _initialDisplayName) {
      try {
        final String updated = await ref
            .read(onlineAccountServiceProvider)
            .updateDisplayName(displayName: trimmedName);
        if (mounted) {
          ref.read(userDisplayNameProvider.notifier).state = updated;
        }
      } catch (e) {
        feedback =
            'Đã lưu cài đặt trên máy. Chưa đồng bộ tên lên máy chủ: ${OnlineAccountService.formatAccountErrorVi(e)}';
      }
    }

    if (mounted) {
      setState(() => _saving = false);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(feedback)),
      );
      if (Navigator.canPop(context)) {
        Navigator.of(context).pop(true);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final List<String> timezones = <String>{
      ..._timezoneOptions,
      _timezone,
    }.toList();
    final List<String> currencies = <String>{
      ..._currencyOptions,
      _currency,
    }.toList();

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: EdgeInsets.fromLTRB(
          20,
          16,
          20,
          20 + MediaQuery.viewInsetsOf(context).bottom,
        ),
        child: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: <Widget>[
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: <Widget>[
                  Text(
                    'Thông tin cá nhân & Múi giờ',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                  IconButton(
                    icon: const Icon(Icons.close_rounded),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_profile_display_name'),
                controller: _displayNameCtrl,
                decoration: const InputDecoration(
                  labelText: 'Tên hiển thị',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                key: const Key('dropdown_personal_timezone'),
                initialValue: _timezone,
                decoration: const InputDecoration(
                  labelText: 'Múi giờ sinh hoạt',
                  border: OutlineInputBorder(),
                ),
                items: timezones
                    .map(
                      (String tz) => DropdownMenuItem<String>(
                        value: tz,
                        child: Text(tz),
                      ),
                    )
                    .toList(),
                onChanged: (String? v) {
                  if (v != null) setState(() => _timezone = v);
                },
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<String>(
                key: const Key('dropdown_personal_currency'),
                initialValue: _currency,
                decoration: const InputDecoration(
                  labelText: 'Tiền tệ mặc định',
                  border: OutlineInputBorder(),
                ),
                items: currencies
                    .map(
                      (String c) => DropdownMenuItem<String>(
                        value: c,
                        child: Text(c),
                      ),
                    )
                    .toList(),
                onChanged: (String? v) {
                  if (v != null) setState(() => _currency = v);
                },
              ),
              const SizedBox(height: 12),
              DropdownButtonFormField<int>(
                key: const Key('dropdown_personal_week_start'),
                initialValue: _weekStartDay,
                decoration: const InputDecoration(
                  labelText: 'Ngày bắt đầu tuần',
                  border: OutlineInputBorder(),
                ),
                items: const <DropdownMenuItem<int>>[
                  DropdownMenuItem<int>(value: 1, child: Text('Thứ Hai')),
                  DropdownMenuItem<int>(value: 7, child: Text('Chủ Nhật')),
                ],
                onChanged: (int? v) {
                  if (v != null) setState(() => _weekStartDay = v);
                },
              ),
              const SizedBox(height: 16),
              FilledButton(
                key: const Key('btn_save_personal_settings'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _saving ? null : _save,
                child: Text(_saving ? 'Đang lưu...' : 'Lưu thay đổi'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Sheet quản lý thiết bị & phiên đăng nhập (`GET /devices`, `DELETE /devices/{id}`).
class DevicesSheet extends ConsumerStatefulWidget {
  const DevicesSheet({super.key});

  @override
  ConsumerState<DevicesSheet> createState() => _DevicesSheetState();
}

class _DevicesSheetState extends ConsumerState<DevicesSheet> {
  List<Map<String, dynamic>> _devices = <Map<String, dynamic>>[];
  bool _loading = true;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadDevices();
  }

  Future<void> _loadDevices() async {
    setState(() {
      _loading = true;
      _errorMessage = null;
    });
    try {
      final List<Map<String, dynamic>> items =
          await ref.read(onlineAccountServiceProvider).listDevices();
      if (mounted) {
        setState(() {
          _devices = items;
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = OnlineAccountService.formatAccountErrorVi(e);
          _loading = false;
        });
      }
    }
  }

  Future<void> _revokeDevice(Map<String, dynamic> device) async {
    final String deviceId =
        (device['id'] ?? device['deviceId'] ?? '').toString();
    final String deviceName =
        (device['name'] ?? device['deviceName'] ?? 'Thiết bị').toString();
    if (deviceId.isEmpty) return;

    final bool? confirmed = await showDialog<bool>(
      context: context,
      builder: (BuildContext ctx) => AlertDialog(
        title: const Text('Thu hồi quyền thiết bị?'),
        content: Text(
          'Phiên đăng nhập trên "$deviceName" sẽ bị chấm dứt và cần đăng nhập lại nếu muốn tiếp tục đồng bộ.',
        ),
        actions: <Widget>[
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(ViStrings.cancelAction),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              backgroundColor: LifeOsTokens.of(ctx).danger,
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Thu hồi'),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;

    try {
      await ref.read(onlineAccountServiceProvider).revokeDevice(deviceId);
      if (mounted) {
        setState(() {
          _devices.removeWhere(
            (Map<String, dynamic> d) =>
                (d['id'] ?? d['deviceId'] ?? '').toString() == deviceId,
          );
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Đã thu hồi phiên của "$deviceName".')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(OnlineAccountService.formatAccountErrorVi(e))),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * 0.82,
        ),
        child: Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.fromLTRB(
                LifeOsTokens.space4,
                LifeOsTokens.space3,
                LifeOsTokens.space2,
                LifeOsTokens.space2,
              ),
              child: Row(
                children: <Widget>[
                  Icon(Icons.devices_rounded, color: tokens.accentPrimary),
                  const SizedBox(width: LifeOsTokens.space2),
                  Expanded(
                    child: Text(
                      'Thiết bị & Phiên đăng nhập',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close_rounded),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
            ),
            const Divider(height: 1),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator())
                  : _errorMessage != null
                      ? Center(
                          child: Padding(
                            padding: const EdgeInsets.all(LifeOsTokens.space4),
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: <Widget>[
                                LifeOsErrorBanner(message: _errorMessage!),
                                const SizedBox(height: 12),
                                OutlinedButton(
                                  onPressed: _loadDevices,
                                  child: const Text('Thử lại'),
                                ),
                              ],
                            ),
                          ),
                        )
                      : _devices.isEmpty
                          ? const Center(
                              child: Padding(
                                padding: EdgeInsets.all(LifeOsTokens.space4),
                                child: LifeOsEmptyState(
                                  icon: Icons.devices_other_outlined,
                                  title: 'Không có thiết bị nào được ghi nhận',
                                  subtitle:
                                      'Danh sách phiên đăng nhập đang hoạt động sẽ hiển thị tại đây.',
                                ),
                              ),
                            )
                          : ListView.separated(
                              padding: const EdgeInsets.all(LifeOsTokens.space4),
                              itemCount: _devices.length,
                              separatorBuilder: (_, __) =>
                                  const SizedBox(height: 8),
                              itemBuilder: (BuildContext ctx, int idx) {
                                final Map<String, dynamic> d = _devices[idx];
                                final String id =
                                    (d['id'] ?? d['deviceId'] ?? idx).toString();
                                final String name = (d['name'] ??
                                        d['deviceName'] ??
                                        'Thiết bị không rõ tên')
                                    .toString();
                                final String platform =
                                    (d['platform'] ?? '').toString();
                                final String version =
                                    (d['appVersion'] ?? '').toString();
                                final String lastSeen =
                                    (d['lastSeenAt'] ?? '').toString();
                                final bool isCurrent = d['current'] == true;

                                final List<String> meta = <String>[
                                  if (platform.isNotEmpty) platform,
                                  if (version.isNotEmpty) 'v$version',
                                  if (lastSeen.isNotEmpty)
                                    lastSeen.length >= 16
                                        ? lastSeen.substring(0, 16)
                                        : lastSeen,
                                ];

                                return LifeOsCard(
                                  elevation: 1,
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 12,
                                    vertical: 8,
                                  ),
                                  child: Row(
                                    children: <Widget>[
                                      Icon(
                                        Icons.phone_android_rounded,
                                        color: isCurrent
                                            ? tokens.accentPrimary
                                            : tokens.textSecondary,
                                      ),
                                      const SizedBox(width: 12),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment:
                                              CrossAxisAlignment.start,
                                          children: <Widget>[
                                            Text(
                                              isCurrent
                                                  ? '$name (Thiết bị này)'
                                                  : name,
                                              style: TextStyle(
                                                fontWeight: FontWeight.w700,
                                                color: tokens.textPrimary,
                                              ),
                                            ),
                                            if (meta.isNotEmpty)
                                              Text(
                                                meta.join(' · '),
                                                style: TextStyle(
                                                  fontSize: 12,
                                                  color: tokens.textSecondary,
                                                ),
                                              ),
                                          ],
                                        ),
                                      ),
                                      if (!isCurrent)
                                        TextButton(
                                          key: Key('btn_revoke_device_$id'),
                                          style: TextButton.styleFrom(
                                            foregroundColor: tokens.danger,
                                          ),
                                          onPressed: () => _revokeDevice(d),
                                          child: const Text('Thu hồi'),
                                        ),
                                    ],
                                  ),
                                );
                              },
                            ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Sheet Xuất dữ liệu tài khoản & Yêu cầu xóa tài khoản chờ 7 ngày.
class AccountPrivacySheet extends ConsumerStatefulWidget {
  const AccountPrivacySheet({super.key});

  @override
  ConsumerState<AccountPrivacySheet> createState() =>
      _AccountPrivacySheetState();
}

class _AccountPrivacySheetState extends ConsumerState<AccountPrivacySheet> {
  final TextEditingController _passwordCtrl = TextEditingController();
  final TextEditingController _codeCtrl = TextEditingController();

  String? _exportId;
  String? _exportStatus;
  bool _busyExport = false;
  int? _downloadedBytes;

  bool _codeSent = false;
  bool _busyDeletion = false;
  String? _deletionScheduledAt;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadDeletionStatus();
  }

  @override
  void dispose() {
    _passwordCtrl.dispose();
    _codeCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadDeletionStatus() async {
    try {
      final Map<String, dynamic> status =
          await ref.read(onlineAccountServiceProvider).getAccountDeletionStatus();
      if (!mounted) return;
      final String? scheduled = status['deletionScheduledAt'] as String?;
      final bool pending = status['pending'] == true || scheduled != null;
      if (pending) {
        setState(() {
          _deletionScheduledAt = scheduled ?? 'đang chờ xử lý';
        });
        ref.read(accountDeletionPendingBannerProvider.notifier).state = true;
      }
    } catch (_) {
      // Bỏ qua khi ngoại tuyến hoặc chưa có yêu cầu xóa nào.
    }
  }

  Future<void> _requestExport() async {
    if (_busyExport) return;
    setState(() {
      _busyExport = true;
      _errorMessage = null;
      _downloadedBytes = null;
    });
    try {
      final Map<String, dynamic> resp =
          await ref.read(onlineAccountServiceProvider).requestDataExport();
      if (mounted) {
        setState(() {
          _exportId = (resp['id'] ?? resp['exportId'])?.toString();
          _exportStatus = (resp['status'] as String?) ?? 'PENDING';
          _busyExport = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _busyExport = false;
          _errorMessage = OnlineAccountService.formatAccountErrorVi(e);
        });
      }
    }
  }

  Future<void> _refreshExportStatus() async {
    if (_exportId == null || _busyExport) return;
    setState(() {
      _busyExport = true;
      _errorMessage = null;
    });
    try {
      final Map<String, dynamic> resp = await ref
          .read(onlineAccountServiceProvider)
          .getDataExportStatus(_exportId!);
      if (mounted) {
        setState(() {
          _exportStatus = (resp['status'] as String?) ?? _exportStatus;
          _busyExport = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _busyExport = false;
          _errorMessage = OnlineAccountService.formatAccountErrorVi(e);
        });
      }
    }
  }

  Future<void> _downloadExport() async {
    if (_exportId == null || _busyExport) return;
    setState(() {
      _busyExport = true;
      _errorMessage = null;
    });
    try {
      final Uint8List bytes = await ref
          .read(onlineAccountServiceProvider)
          .downloadDataExportZip(_exportId!);
      if (mounted) {
        setState(() {
          _busyExport = false;
          _downloadedBytes = bytes.length;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Đã tải tệp sao lưu tài khoản (${bytes.length} byte).'),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _busyExport = false;
          _errorMessage = OnlineAccountService.formatAccountErrorVi(e);
        });
      }
    }
  }

  Future<void> _sendDeletionCode() async {
    if (_busyDeletion) return;
    setState(() {
      _busyDeletion = true;
      _errorMessage = null;
    });
    try {
      await ref
          .read(onlineAccountServiceProvider)
          .requestAccountDeletionCode();
      if (mounted) {
        setState(() {
          _busyDeletion = false;
          _codeSent = true;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Đã gửi mã xác nhận 6 chữ số tới email tài khoản.'),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _busyDeletion = false;
          _errorMessage = OnlineAccountService.formatAccountErrorVi(e);
        });
      }
    }
  }

  Future<void> _confirmDeletion() async {
    if (_busyDeletion) return;
    final String pwd = _passwordCtrl.text;
    final String code = _codeCtrl.text.trim();
    if (pwd.isEmpty || code.length < 6) {
      setState(() {
        _errorMessage = 'Vui lòng nhập mật khẩu và mã xác nhận 6 chữ số.';
      });
      return;
    }

    setState(() {
      _busyDeletion = true;
      _errorMessage = null;
    });
    try {
      final Map<String, dynamic> resp = await ref
          .read(onlineAccountServiceProvider)
          .confirmAccountDeletion(password: pwd, sixDigitCode: code);
      if (mounted) {
        ref.read(accountDeletionPendingBannerProvider.notifier).state = true;
        setState(() {
          _busyDeletion = false;
          _deletionScheduledAt =
              (resp['deletionScheduledAt'] as String?) ?? '7 ngày tới';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _busyDeletion = false;
          _errorMessage = OnlineAccountService.formatAccountErrorVi(e);
        });
      }
    }
  }

  Future<void> _cancelDeletion() async {
    if (_busyDeletion) return;
    setState(() {
      _busyDeletion = true;
      _errorMessage = null;
    });
    try {
      await ref.read(onlineAccountServiceProvider).cancelAccountDeletion();
      if (mounted) {
        ref.read(accountDeletionPendingBannerProvider.notifier).state = false;
        setState(() {
          _busyDeletion = false;
          _deletionScheduledAt = null;
          _codeSent = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã hủy yêu cầu xóa tài khoản.')),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _busyDeletion = false;
          _errorMessage = OnlineAccountService.formatAccountErrorVi(e);
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final bool exportReady = _exportStatus == 'READY' || _exportStatus == 'COMPLETED';

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * 0.88,
        ),
        child: Padding(
          padding: EdgeInsets.fromLTRB(
            20,
            16,
            20,
            20 + MediaQuery.viewInsetsOf(context).bottom,
          ),
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: <Widget>[
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: <Widget>[
                    Text(
                      'Xuất dữ liệu & Xóa tài khoản',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                    IconButton(
                      icon: const Icon(Icons.close_rounded),
                      onPressed: () => Navigator.of(context).pop(),
                    ),
                  ],
                ),
                if (_errorMessage != null) ...<Widget>[
                  const SizedBox(height: 8),
                  LifeOsErrorBanner(message: _errorMessage!),
                ],
                const SizedBox(height: 12),
                LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: <Widget>[
                      Text(
                        'Xuất bản sao dữ liệu tài khoản (.zip)',
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          color: tokens.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Bản xuất chỉ bao gồm dữ liệu đồng bộ trên tài khoản máy chủ (không gồm Bánh xe cuộc sống và Khu vườn tập trung chỉ lưu trên máy).',
                        style: TextStyle(
                          fontSize: 12.5,
                          color: tokens.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 10),
                      FilledButton.tonalIcon(
                        key: const Key('btn_request_data_export'),
                        onPressed: _busyExport ? null : _requestExport,
                        icon: const Icon(Icons.archive_outlined, size: 18),
                        label: const Text('Yêu cầu xuất dữ liệu'),
                      ),
                      if (_exportId != null) ...<Widget>[
                        const SizedBox(height: 8),
                        Text(
                          'Trạng thái: $_exportStatus${_downloadedBytes != null ? ' · Đã tải $_downloadedBytes byte' : ''}',
                          style: TextStyle(
                            fontSize: 12.5,
                            fontWeight: FontWeight.w600,
                            color: tokens.accentPrimary,
                          ),
                        ),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          runSpacing: 8,
                          children: <Widget>[
                            OutlinedButton(
                              key: const Key('btn_check_export_status'),
                              onPressed:
                                  _busyExport ? null : _refreshExportStatus,
                              child: const Text('Kiểm tra trạng thái'),
                            ),
                            if (exportReady)
                              FilledButton.icon(
                                key: const Key('btn_download_data_export'),
                                onPressed: _busyExport ? null : _downloadExport,
                                icon: const Icon(Icons.download_rounded, size: 18),
                                label: const Text('Tải bản sao (.zip)'),
                              ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(height: 14),
                LifeOsCard(
                  elevation: 1,
                  padding: const EdgeInsets.all(LifeOsTokens.space3),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: <Widget>[
                      Text(
                        'Xóa tài khoản (thời gian chờ 7 ngày)',
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          color: tokens.danger,
                        ),
                      ),
                      const SizedBox(height: 6),
                      if (_deletionScheduledAt != null) ...<Widget>[
                        Text(
                          'Tài khoản đang hẹn xóa vĩnh viễn vào: $_deletionScheduledAt. Bạn vẫn có thể hủy yêu cầu trước thời hạn.',
                          style: TextStyle(
                            fontSize: 13,
                            color: tokens.textPrimary,
                          ),
                        ),
                        const SizedBox(height: 10),
                        FilledButton(
                          key: const Key('btn_cancel_account_deletion'),
                          onPressed: _busyDeletion ? null : _cancelDeletion,
                          child: const Text('Hủy yêu cầu xóa tài khoản'),
                        ),
                      ] else ...<Widget>[
                        Text(
                          'Sau khi xác nhận, tài khoản sẽ vào giai đoạn chờ 7 ngày trước khi bị xóa vĩnh viễn trên máy chủ.',
                          style: TextStyle(
                            fontSize: 12.5,
                            color: tokens.textSecondary,
                          ),
                        ),
                        const SizedBox(height: 10),
                        OutlinedButton.icon(
                          key: const Key('btn_send_deletion_code'),
                          onPressed: _busyDeletion ? null : _sendDeletionCode,
                          icon: const Icon(Icons.mark_email_read_outlined, size: 18),
                          label: Text(
                            _codeSent
                                ? 'Gửi lại mã xác nhận qua email'
                                : 'Bước 1: Gửi mã 6 chữ số qua email',
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextField(
                          key: const Key('input_deletion_password'),
                          controller: _passwordCtrl,
                          obscureText: true,
                          decoration: const InputDecoration(
                            labelText: 'Mật khẩu hiện tại',
                            border: OutlineInputBorder(),
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextField(
                          key: const Key('input_deletion_code'),
                          controller: _codeCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                            labelText: 'Mã xác nhận 6 chữ số',
                            hintText: '123456',
                            border: OutlineInputBorder(),
                          ),
                        ),
                        const SizedBox(height: 12),
                        FilledButton(
                          key: const Key('btn_confirm_account_deletion'),
                          style: FilledButton.styleFrom(
                            backgroundColor: tokens.danger,
                            foregroundColor: Colors.white,
                            minimumSize: const Size.fromHeight(
                              LifeOsTokens.minTouchTarget,
                            ),
                          ),
                          onPressed: _busyDeletion ? null : _confirmDeletion,
                          child: const Text('Bước 2: Xác nhận xóa tài khoản'),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// Sheet Hộp thư thông báo (`GET /notifications`, `POST /notifications/{id}/read`, `/read-all`).
class NotificationsInboxSheet extends ConsumerStatefulWidget {
  const NotificationsInboxSheet({super.key});

  @override
  ConsumerState<NotificationsInboxSheet> createState() =>
      _NotificationsInboxSheetState();
}

class _NotificationsInboxSheetState
    extends ConsumerState<NotificationsInboxSheet> {
  List<Map<String, dynamic>> _items = <Map<String, dynamic>>[];
  String? _nextCursor;
  bool _hasMore = false;
  bool _unreadOnly = false;
  bool _loading = true;
  bool _loadingMore = false;
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadNotifications();
  }

  Future<void> _loadNotifications() async {
    setState(() {
      _loading = true;
      _errorMessage = null;
    });

    try {
      final Response<dynamic> resp = await ref.read(apiClientProvider).dio.get<dynamic>(
        '/notifications',
        queryParameters: <String, dynamic>{
          'limit': 30,
          if (_unreadOnly) 'unreadOnly': true,
        },
      );
      final Map<String, dynamic> body =
          (resp.data as Map<dynamic, dynamic>? ?? <dynamic, dynamic>{})
              .cast<String, dynamic>();
      final List<dynamic> rawItems =
          (body['items'] as List<dynamic>?) ?? const <dynamic>[];
      if (mounted) {
        setState(() {
          _items = rawItems
              .map(
                (dynamic e) =>
                    (e as Map<dynamic, dynamic>).cast<String, dynamic>(),
              )
              .toList();
          _nextCursor = body['nextCursor'] as String?;
          _hasMore = body['hasMore'] == true;
          _loading = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _errorMessage = LifeOsApiClient.normalizeError(e).userMessageVi;
          _loading = false;
        });
      }
    }
  }

  Future<void> _loadMore() async {
    if (_loadingMore || !_hasMore || _nextCursor == null) return;
    setState(() => _loadingMore = true);
    try {
      final Response<dynamic> resp = await ref.read(apiClientProvider).dio.get<dynamic>(
        '/notifications',
        queryParameters: <String, dynamic>{
          'limit': 30,
          'cursor': _nextCursor,
          if (_unreadOnly) 'unreadOnly': true,
        },
      );
      final Map<String, dynamic> body =
          (resp.data as Map<dynamic, dynamic>? ?? <dynamic, dynamic>{})
              .cast<String, dynamic>();
      final List<dynamic> rawItems =
          (body['items'] as List<dynamic>?) ?? const <dynamic>[];
      if (mounted) {
        final Set<String> seen =
            _items.map((Map<String, dynamic> it) => (it['id'] ?? '').toString()).toSet();
        final List<Map<String, dynamic>> appended = rawItems
            .map(
              (dynamic e) =>
                  (e as Map<dynamic, dynamic>).cast<String, dynamic>(),
            )
            .where((Map<String, dynamic> it) => !seen.contains((it['id'] ?? '').toString()))
            .toList();
        setState(() {
          _items.addAll(appended);
          _nextCursor = body['nextCursor'] as String?;
          _hasMore = body['hasMore'] == true;
          _loadingMore = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _loadingMore = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(LifeOsApiClient.normalizeError(e).userMessageVi)),
        );
      }
    }
  }

  Future<void> _markAllRead() async {
    try {
      await ref.read(apiClientProvider).dio.post<dynamic>('/notifications/read-all');
      if (mounted) {
        final String nowIso =
            ref.read(referenceUtcProvider).toIso8601String();
        setState(() {
          if (_unreadOnly) {
            _items.clear();
          } else {
            for (final Map<String, dynamic> it in _items) {
              it['readAt'] ??= nowIso;
            }
          }
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(LifeOsApiClient.normalizeError(e).userMessageVi)),
        );
      }
    }
  }

  Future<void> _markRead(Map<String, dynamic> item) async {
    final String id = (item['id'] ?? '').toString();
    if (id.isEmpty || item['readAt'] != null) return;
    try {
      await ref
          .read(apiClientProvider)
          .dio
          .post<dynamic>('/notifications/$id/read');
      if (mounted) {
        setState(() {
          item['readAt'] = ref.read(referenceUtcProvider).toIso8601String();
          if (_unreadOnly) {
            _items.removeWhere(
              (Map<String, dynamic> it) => (it['id'] ?? '').toString() == id,
            );
          }
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(LifeOsApiClient.normalizeError(e).userMessageVi)),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: ConstrainedBox(
        constraints: BoxConstraints(
          maxHeight: MediaQuery.sizeOf(context).height * 0.85,
        ),
        child: Column(
          children: <Widget>[
            Padding(
              padding: const EdgeInsets.fromLTRB(
                LifeOsTokens.space4,
                LifeOsTokens.space3,
                LifeOsTokens.space2,
                LifeOsTokens.space2,
              ),
              child: Row(
                children: <Widget>[
                  Icon(Icons.notifications_none_rounded,
                      color: tokens.accentPrimary),
                  const SizedBox(width: LifeOsTokens.space2),
                  Expanded(
                    child: Text(
                      'Hộp thư thông báo',
                      style: Theme.of(context).textTheme.titleLarge,
                    ),
                  ),
                  TextButton(
                    key: const Key('btn_mark_all_notifications_read'),
                    onPressed: _items.isEmpty ? null : _markAllRead,
                    child: const Text('Đọc hết'),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close_rounded),
                    onPressed: () => Navigator.of(context).pop(),
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.symmetric(
                horizontal: LifeOsTokens.space4,
                vertical: 4,
              ),
              child: Row(
                children: <Widget>[
                  ChoiceChip(
                    key: const Key('chip_notif_all'),
                    label: const Text('Tất cả'),
                    selected: !_unreadOnly,
                    onSelected: (_) {
                      if (_unreadOnly) {
                        setState(() => _unreadOnly = false);
                        _loadNotifications();
                      }
                    },
                  ),
                  const SizedBox(width: 8),
                  ChoiceChip(
                    key: const Key('chip_notif_unread'),
                    label: const Text('Chưa đọc'),
                    selected: _unreadOnly,
                    onSelected: (_) {
                      if (!_unreadOnly) {
                        setState(() => _unreadOnly = true);
                        _loadNotifications();
                      }
                    },
                  ),
                ],
              ),
            ),
            const Divider(height: 1),
            Expanded(
              child: _loading
                  ? const Center(child: CircularProgressIndicator())
                  : _errorMessage != null
                      ? Center(
                          child: Padding(
                            padding: const EdgeInsets.all(LifeOsTokens.space4),
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: <Widget>[
                                LifeOsErrorBanner(message: _errorMessage!),
                                const SizedBox(height: 12),
                                OutlinedButton(
                                  onPressed: _loadNotifications,
                                  child: const Text('Thử lại'),
                                ),
                              ],
                            ),
                          ),
                        )
                      : _items.isEmpty
                          ? const Center(
                              child: Padding(
                                padding: EdgeInsets.all(LifeOsTokens.space4),
                                child: LifeOsEmptyState(
                                  icon: Icons.notifications_off_outlined,
                                  title: 'Không có thông báo nào',
                                  subtitle:
                                      'Các nhắc việc, cảnh báo ngân sách và thông báo hệ thống sẽ xuất hiện tại đây.',
                                ),
                              ),
                            )
                          : ListView.separated(
                              itemCount: _items.length + (_hasMore ? 1 : 0),
                              separatorBuilder: (_, __) =>
                                  const Divider(height: 1),
                              itemBuilder: (BuildContext ctx, int idx) {
                                if (_hasMore && idx == _items.length) {
                                  return Padding(
                                    padding: const EdgeInsets.all(
                                        LifeOsTokens.space3),
                                    child: OutlinedButton(
                                      key: const Key(
                                          'btn_load_more_notifications'),
                                      onPressed:
                                          _loadingMore ? null : _loadMore,
                                      child: Text(
                                        _loadingMore ? 'Đang tải...' : 'Tải thêm',
                                      ),
                                    ),
                                  );
                                }
                                final Map<String, dynamic> it = _items[idx];
                                final String id = (it['id'] ?? idx).toString();
                                final bool isUnread = it['readAt'] == null;
                                final String title =
                                    ViStrings.formatNotificationTemplate(
                                  it['templateKey'] as String?,
                                  fallbackTitle: it['title'] as String?,
                                );
                                final String bodyText =
                                    (it['body'] as String?)?.trim() ?? '';
                                final String createdAt =
                                    (it['createdAt'] as String?) ?? '';

                                return ListTile(
                                  key: Key('notif_item_$id'),
                                  leading: Icon(
                                    isUnread
                                        ? Icons.mark_email_unread_rounded
                                        : Icons.notifications_none_rounded,
                                    color: isUnread
                                        ? tokens.accentPrimary
                                        : tokens.textMuted,
                                  ),
                                  title: Text(
                                    title,
                                    style: TextStyle(
                                      fontWeight: isUnread
                                          ? FontWeight.w700
                                          : FontWeight.w500,
                                      color: tokens.textPrimary,
                                    ),
                                  ),
                                  subtitle: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: <Widget>[
                                      if (bodyText.isNotEmpty) Text(bodyText),
                                      if (createdAt.isNotEmpty)
                                        Text(
                                          createdAt.length >= 16
                                              ? createdAt.substring(0, 16)
                                              : createdAt,
                                          style: TextStyle(
                                            fontSize: 11,
                                            color: tokens.textMuted,
                                          ),
                                        ),
                                    ],
                                  ),
                                  onTap: () => _markRead(it),
                                );
                              },
                            ),
            ),
          ],
        ),
      ),
    );
  }
}
