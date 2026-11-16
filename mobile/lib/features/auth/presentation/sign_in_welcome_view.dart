import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/vi_strings.dart';
import '../../../core/network/api_client.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../data/online_account_service.dart';
import 'account_flows_sheets.dart';

/// Màn hình Tài khoản & Đăng nhập đầy đủ theo hướng Botanical Premium (`MWP-B`):
/// - Đăng nhập với validate blur-first, nhãn hiện rõ, vùng chạm >= 48dp.
/// - Ánh xạ thông điệp lỗi tiếng Việt chuẩn từ OpenAPI (không lộ mã kỹ thuật), báo rõ khi ngoại tuyến.
/// - Hỗ trợ Đăng ký + Xác minh email, Quên mật khẩu, hoặc Tiếp tục dùng ngoại tuyến (giữ SQLite Phương án C).
class SignInWelcomeView extends ConsumerStatefulWidget {
  const SignInWelcomeView({super.key});

  @override
  ConsumerState<SignInWelcomeView> createState() => _SignInWelcomeViewState();
}

class _SignInWelcomeViewState extends ConsumerState<SignInWelcomeView> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final FocusNode _emailFocus = FocusNode();
  final FocusNode _passwordFocus = FocusNode();

  bool _obscurePassword = true;
  bool _loading = false;
  String? _emailError;
  String? _passwordError;
  String? _errorText;
  String? _infoText;
  bool _showVerifyPrompt = false;

  @override
  void initState() {
    super.initState();
    _emailFocus.addListener(() {
      if (!_emailFocus.hasFocus && mounted) {
        setState(() => _emailError = _validateEmail());
      }
    });
    _passwordFocus.addListener(() {
      if (!_passwordFocus.hasFocus && mounted) {
        setState(() => _passwordError = _validatePassword());
      }
    });
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _emailFocus.dispose();
    _passwordFocus.dispose();
    super.dispose();
  }

  String? _validateEmail() {
    if (_emailController.text.trim().isEmpty) {
      return 'Vui lòng nhập email.';
    }
    if (!ViStrings.isValidEmail(_emailController.text)) {
      return 'Địa chỉ email chưa đúng định dạng.';
    }
    return null;
  }

  String? _validatePassword() {
    if (_passwordController.text.isEmpty) {
      return 'Vui lòng nhập mật khẩu.';
    }
    return null;
  }

  Future<void> _submitLogin() async {
    final String? emailErr = _validateEmail();
    final String? passErr = _validatePassword();
    setState(() {
      _emailError = emailErr;
      _passwordError = passErr;
      _errorText = null;
      _infoText = null;
      _showVerifyPrompt = false;
    });
    if (emailErr != null || passErr != null) return;

    setState(() => _loading = true);
    try {
      final OnlineAccountService authService =
          ref.read(onlineAccountServiceProvider);
      final res = await authService.login(
        email: _emailController.text.trim(),
        password: _passwordController.text,
        deviceName: 'LifeOS Mobile',
      );
      final String resolvedName = res.displayName.trim().isNotEmpty
          ? res.displayName.trim()
          : 'bạn';
      ref.read(currentUserIdProvider.notifier).state = res.userId;
      ref.read(userDisplayNameProvider.notifier).state = resolvedName;
      ref.read(requiresReAuthBannerProvider.notifier).state = false;
      ref.read(accountDeletionPendingBannerProvider.notifier).state = false;
      ref.read(isSignedOutProvider.notifier).state = false;
    } catch (e) {
      final LifeOsApiException apiErr = LifeOsApiClient.normalizeError(e);
      if (mounted) {
        setState(() {
          _errorText = OnlineAccountService.formatAccountErrorVi(apiErr);
          _showVerifyPrompt = apiErr.errorCode == 'AUTH_EMAIL_NOT_VERIFIED' ||
              apiErr.errorCode == 'EMAIL_NOT_VERIFIED';
        });
      }
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _openRegisterSheet({bool startAtVerify = false}) async {
    final String? verifiedEmail = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext ctx) => RegisterAndVerifySheet(
        initialEmail: _emailController.text.trim(),
        startAtVerifyStep: startAtVerify,
      ),
    );
    if (verifiedEmail != null && mounted) {
      _emailController.text = verifiedEmail;
      setState(() {
        _errorText = null;
        _showVerifyPrompt = false;
        _infoText = 'Email đã được xác minh! Bạn có thể đăng nhập ngay.';
      });
    }
  }

  Future<void> _openForgotPasswordSheet() async {
    final bool? resetDone = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      useSafeArea: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext ctx) => ForgotPasswordSheet(
        initialEmail: _emailController.text.trim(),
      ),
    );
    if (resetDone == true && mounted) {
      setState(() {
        _errorText = null;
        _infoText = 'Đã đặt lại mật khẩu thành công. Hãy đăng nhập với mật khẩu mới.';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(LifeOsTokens.space6),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: LifeOsCard(
                elevation: 2,
                padding: const EdgeInsets.all(LifeOsTokens.space6),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: <Widget>[
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: tokens.accentPrimary.withValues(alpha: 0.12),
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        Icons.spa_rounded,
                        size: 36,
                        color: tokens.accentPrimary,
                      ),
                    ),
                    const SizedBox(height: LifeOsTokens.space4),
                    Text(
                      'Chào mừng trở lại LifeOS',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.w800,
                        color: tokens.textPrimary,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Đăng nhập để đồng bộ không gian sống cá nhân hoặc tiếp tục dùng ngoại tuyến trên thiết bị này.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 13.5,
                        color: tokens.textSecondary,
                        height: 1.4,
                      ),
                    ),
                    const SizedBox(height: LifeOsTokens.space4),
                    if (_errorText != null) ...<Widget>[
                      LifeOsErrorBanner(message: _errorText!),
                      if (_showVerifyPrompt) ...<Widget>[
                        const SizedBox(height: 6),
                        Align(
                          alignment: Alignment.centerLeft,
                          child: TextButton(
                            onPressed: () =>
                                _openRegisterSheet(startAtVerify: true),
                            child: const Text('Nhập mã hoặc gửi lại email xác minh'),
                          ),
                        ),
                      ],
                      const SizedBox(height: LifeOsTokens.space3),
                    ],
                    if (_infoText != null) ...<Widget>[
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: tokens.success.withValues(alpha: 0.12),
                          borderRadius:
                              BorderRadius.circular(LifeOsTokens.radiusMd),
                        ),
                        child: Text(
                          _infoText!,
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            color: tokens.success,
                          ),
                        ),
                      ),
                      const SizedBox(height: LifeOsTokens.space3),
                    ],
                    TextField(
                      key: const Key('input_login_email'),
                      controller: _emailController,
                      focusNode: _emailFocus,
                      keyboardType: TextInputType.emailAddress,
                      textInputAction: TextInputAction.next,
                      decoration: InputDecoration(
                        labelText: 'Email *',
                        hintText: 'ban@example.com',
                        errorText: _emailError,
                        prefixIcon: const Icon(Icons.email_outlined),
                        border: const OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: LifeOsTokens.space3),
                    TextField(
                      key: const Key('input_login_password'),
                      controller: _passwordController,
                      focusNode: _passwordFocus,
                      obscureText: _obscurePassword,
                      textInputAction: TextInputAction.done,
                      onSubmitted: (_) => _loading ? null : _submitLogin(),
                      decoration: InputDecoration(
                        labelText: 'Mật khẩu *',
                        errorText: _passwordError,
                        prefixIcon: const Icon(Icons.lock_outline_rounded),
                        border: const OutlineInputBorder(),
                        suffixIcon: IconButton(
                          tooltip:
                              _obscurePassword ? 'Hiện mật khẩu' : 'Ẩn mật khẩu',
                          onPressed: () => setState(
                            () => _obscurePassword = !_obscurePassword,
                          ),
                          icon: Icon(
                            _obscurePassword
                                ? Icons.visibility_outlined
                                : Icons.visibility_off_outlined,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton(
                        key: const Key('btn_open_forgot_password'),
                        style: TextButton.styleFrom(
                          minimumSize:
                              const Size(48, LifeOsTokens.minTouchTarget),
                        ),
                        onPressed: _loading ? null : _openForgotPasswordSheet,
                        child: const Text('Quên mật khẩu?'),
                      ),
                    ),
                    const SizedBox(height: LifeOsTokens.space2),
                    FilledButton(
                      key: const Key('btn_submit_login'),
                      style: FilledButton.styleFrom(
                        minimumSize:
                            const Size.fromHeight(LifeOsTokens.minTouchTarget),
                      ),
                      onPressed: _loading ? null : _submitLogin,
                      child: _loading
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text('Đăng nhập'),
                    ),
                    const SizedBox(height: LifeOsTokens.space2),
                    FilledButton.tonal(
                      key: const Key('btn_open_register'),
                      style: FilledButton.styleFrom(
                        minimumSize:
                            const Size.fromHeight(LifeOsTokens.minTouchTarget),
                      ),
                      onPressed: _loading ? null : () => _openRegisterSheet(),
                      child: const Text('Tạo tài khoản mới'),
                    ),
                    const SizedBox(height: LifeOsTokens.space2),
                    OutlinedButton(
                      key: const Key('btn_continue_offline'),
                      style: OutlinedButton.styleFrom(
                        minimumSize:
                            const Size.fromHeight(LifeOsTokens.minTouchTarget),
                      ),
                      onPressed: _loading
                          ? null
                          : () {
                              ref.read(isSignedOutProvider.notifier).state =
                                  false;
                            },
                      child: const Text('Tiếp tục dùng ngoại tuyến'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
