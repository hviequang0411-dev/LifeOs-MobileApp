import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/vi_strings.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../data/online_account_service.dart';
export 'change_password_dialog.dart';

/// Sheet Đăng ký tài khoản mới + Xác minh email (`POST /auth/register`, `/auth/verify-email`, `/auth/resend-verification`).
class RegisterAndVerifySheet extends ConsumerStatefulWidget {
  const RegisterAndVerifySheet({
    super.key,
    this.initialEmail,
    this.startAtVerifyStep = false,
  });

  final String? initialEmail;
  final bool startAtVerifyStep;

  @override
  ConsumerState<RegisterAndVerifySheet> createState() =>
      _RegisterAndVerifySheetState();
}

class _RegisterAndVerifySheetState
    extends ConsumerState<RegisterAndVerifySheet> {
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _confirmPasswordController =
      TextEditingController();
  final TextEditingController _tokenController = TextEditingController();

  final FocusNode _nameFocus = FocusNode();
  final FocusNode _emailFocus = FocusNode();
  final FocusNode _passwordFocus = FocusNode();
  final FocusNode _confirmFocus = FocusNode();
  final FocusNode _tokenFocus = FocusNode();

  bool _stepVerify = false;
  bool _obscurePassword = true;
  bool _loading = false;
  String? _errorText;
  String? _infoText;

  String? _nameError;
  String? _emailError;
  String? _passwordError;
  String? _confirmError;
  String? _tokenError;

  @override
  void initState() {
    super.initState();
    if (widget.initialEmail != null) {
      _emailController.text = widget.initialEmail!;
    }
    _stepVerify = widget.startAtVerifyStep;
    _attachBlurValidation();
  }

  void _attachBlurValidation() {
    _nameFocus.addListener(() {
      if (!_nameFocus.hasFocus && mounted) {
        setState(() => _nameError = _validateName());
      }
    });
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
    _confirmFocus.addListener(() {
      if (!_confirmFocus.hasFocus && mounted) {
        setState(() => _confirmError = _validateConfirm());
      }
    });
    _tokenFocus.addListener(() {
      if (!_tokenFocus.hasFocus && mounted) {
        setState(() => _tokenError = _validateToken());
      }
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _tokenController.dispose();
    _nameFocus.dispose();
    _emailFocus.dispose();
    _passwordFocus.dispose();
    _confirmFocus.dispose();
    _tokenFocus.dispose();
    super.dispose();
  }

  String? _validateName() => _nameController.text.trim().length < 2
      ? 'Vui lòng nhập tên hiển thị (tối thiểu 2 ký tự).'
      : null;

  String? _validateEmail() => !ViStrings.isValidEmail(_emailController.text)
      ? 'Địa chỉ email chưa đúng định dạng.'
      : null;

  String? _validatePassword() =>
      !ViStrings.isValidPassword(_passwordController.text)
          ? 'Mật khẩu cần tối thiểu 8 ký tự.'
          : null;

  String? _validateConfirm() =>
      _confirmPasswordController.text != _passwordController.text
          ? 'Mật khẩu xác nhận không khớp.'
          : null;

  String? _validateToken() => _tokenController.text.trim().isEmpty
      ? 'Vui lòng nhập mã xác minh đã gửi tới email.'
      : null;

  Future<void> _submitRegister() async {
    setState(() {
      _nameError = _validateName();
      _emailError = _validateEmail();
      _passwordError = _validatePassword();
      _confirmError = _validateConfirm();
      _errorText = null;
      _infoText = null;
    });
    if (_nameError != null ||
        _emailError != null ||
        _passwordError != null ||
        _confirmError != null) {
      return;
    }

    setState(() => _loading = true);
    try {
      await ref.read(onlineAccountServiceProvider).register(
            email: _emailController.text.trim(),
            password: _passwordController.text,
            displayName: _nameController.text.trim(),
          );
      if (mounted) {
        setState(() {
          _stepVerify = true;
          _infoText =
              'Đã tạo tài khoản và gửi email xác minh tới ${_emailController.text.trim()}. Vui lòng dán mã xác minh bên dưới.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _errorText = OnlineAccountService.formatAccountErrorVi(e));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _submitVerify() async {
    setState(() {
      _tokenError = _validateToken();
      _errorText = null;
      _infoText = null;
    });
    if (_tokenError != null) return;

    setState(() => _loading = true);
    try {
      await ref.read(onlineAccountServiceProvider).verifyEmail(
            token: _tokenController.text.trim(),
          );
      if (mounted) {
        Navigator.of(context).pop(_emailController.text.trim());
      }
    } catch (e) {
      if (mounted) {
        setState(() => _errorText = OnlineAccountService.formatAccountErrorVi(e));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _resendVerification() async {
    setState(() {
      _emailError = _validateEmail();
      _errorText = null;
      _infoText = null;
    });
    if (_emailError != null) return;

    setState(() => _loading = true);
    try {
      await ref
          .read(onlineAccountServiceProvider)
          .resendVerification(email: _emailController.text.trim());
      if (mounted) {
        setState(() {
          _infoText =
              'Đã gửi lại email xác minh tới ${_emailController.text.trim()}. Hãy kiểm tra hộp thư (cả mục Thư rác).';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _errorText = OnlineAccountService.formatAccountErrorVi(e));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final double bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: EdgeInsets.fromLTRB(20, 16, 20, 20 + bottomInset),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                Expanded(
                  child: Text(
                    _stepVerify
                        ? 'Xác minh địa chỉ email'
                        : 'Tạo tài khoản LifeOS mới',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                TextButton(
                  onPressed: _loading
                      ? null
                      : () => setState(() {
                            _stepVerify = !_stepVerify;
                            _errorText = null;
                          }),
                  child: Text(_stepVerify ? 'Quay lại đăng ký' : 'Đã có mã xác minh?'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (_errorText != null) ...<Widget>[
              LifeOsErrorBanner(message: _errorText!),
              const SizedBox(height: 10),
            ],
            if (_infoText != null) ...<Widget>[
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: tokens.success.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
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
              const SizedBox(height: 10),
            ],
            if (!_stepVerify) ...<Widget>[
              TextField(
                key: const Key('input_register_name'),
                controller: _nameController,
                focusNode: _nameFocus,
                textInputAction: TextInputAction.next,
                decoration: InputDecoration(
                  labelText: 'Tên hiển thị *',
                  hintText: 'Ví dụ: Minh Anh',
                  errorText: _nameError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_register_email'),
                controller: _emailController,
                focusNode: _emailFocus,
                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.next,
                decoration: InputDecoration(
                  labelText: 'Email *',
                  hintText: 'ban@example.com',
                  errorText: _emailError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_register_password'),
                controller: _passwordController,
                focusNode: _passwordFocus,
                obscureText: _obscurePassword,
                textInputAction: TextInputAction.next,
                decoration: InputDecoration(
                  labelText: 'Mật khẩu (tối thiểu 8 ký tự) *',
                  errorText: _passwordError,
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    tooltip: _obscurePassword ? 'Hiện mật khẩu' : 'Ẩn mật khẩu',
                    onPressed: () =>
                        setState(() => _obscurePassword = !_obscurePassword),
                    icon: Icon(
                      _obscurePassword
                          ? Icons.visibility_outlined
                          : Icons.visibility_off_outlined,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_register_confirm_password'),
                controller: _confirmPasswordController,
                focusNode: _confirmFocus,
                obscureText: _obscurePassword,
                textInputAction: TextInputAction.done,
                decoration: InputDecoration(
                  labelText: 'Nhập lại mật khẩu *',
                  errorText: _confirmError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 16),
              FilledButton(
                key: const Key('btn_submit_register'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _loading ? null : _submitRegister,
                child: _loading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Đăng ký & gửi email xác minh'),
              ),
            ] else ...<Widget>[
              TextField(
                key: const Key('input_verify_email'),
                controller: _emailController,
                focusNode: _emailFocus,
                keyboardType: TextInputType.emailAddress,
                decoration: InputDecoration(
                  labelText: 'Email tài khoản',
                  errorText: _emailError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_verify_token'),
                controller: _tokenController,
                focusNode: _tokenFocus,
                decoration: InputDecoration(
                  labelText: 'Mã xác minh từ email *',
                  hintText: 'Dán mã xác minh nhận được trong hộp thư',
                  errorText: _tokenError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 14),
              FilledButton(
                key: const Key('btn_submit_verify_email'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _loading ? null : _submitVerify,
                child: _loading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Xác minh email & tiếp tục đăng nhập'),
              ),
              const SizedBox(height: 8),
              OutlinedButton(
                key: const Key('btn_resend_verification'),
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _loading ? null : _resendVerification,
                child: const Text('Gửi lại email xác minh'),
              ),
            ],
          ],
        ),
      ),
      ),
    );
  }
}

/// Sheet Quên mật khẩu & Đặt lại mật khẩu (`POST /auth/forgot-password`, `POST /auth/reset-password`).
class ForgotPasswordSheet extends ConsumerStatefulWidget {
  const ForgotPasswordSheet({super.key, this.initialEmail});

  final String? initialEmail;

  @override
  ConsumerState<ForgotPasswordSheet> createState() =>
      _ForgotPasswordSheetState();
}

class _ForgotPasswordSheetState extends ConsumerState<ForgotPasswordSheet> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _tokenController = TextEditingController();
  final TextEditingController _newPasswordController = TextEditingController();
  final FocusNode _emailFocus = FocusNode();
  final FocusNode _tokenFocus = FocusNode();
  final FocusNode _newPassFocus = FocusNode();

  bool _stepReset = false;
  bool _loading = false;
  String? _emailError;
  String? _tokenError;
  String? _newPassError;
  String? _errorText;
  String? _infoText;

  @override
  void initState() {
    super.initState();
    if (widget.initialEmail != null) {
      _emailController.text = widget.initialEmail!;
    }
    _emailFocus.addListener(() {
      if (!_emailFocus.hasFocus && mounted) {
        setState(() {
          _emailError = !ViStrings.isValidEmail(_emailController.text)
              ? 'Email chưa đúng định dạng.'
              : null;
        });
      }
    });
    _tokenFocus.addListener(() {
      if (!_tokenFocus.hasFocus && mounted) {
        setState(() {
          _tokenError = _tokenController.text.trim().isEmpty
              ? 'Vui lòng nhập mã đặt lại mật khẩu từ email.'
              : null;
        });
      }
    });
    _newPassFocus.addListener(() {
      if (!_newPassFocus.hasFocus && mounted) {
        setState(() {
          _newPassError = !ViStrings.isValidPassword(_newPasswordController.text)
              ? 'Mật khẩu mới cần tối thiểu 8 ký tự.'
              : null;
        });
      }
    });
  }

  @override
  void dispose() {
    _emailController.dispose();
    _tokenController.dispose();
    _newPasswordController.dispose();
    _emailFocus.dispose();
    _tokenFocus.dispose();
    _newPassFocus.dispose();
    super.dispose();
  }

  Future<void> _requestResetEmail() async {
    final String? err = !ViStrings.isValidEmail(_emailController.text)
        ? 'Email chưa đúng định dạng.'
        : null;
    setState(() {
      _emailError = err;
      _errorText = null;
      _infoText = null;
    });
    if (err != null) return;

    setState(() => _loading = true);
    try {
      await ref
          .read(onlineAccountServiceProvider)
          .forgotPassword(email: _emailController.text.trim());
      if (mounted) {
        setState(() {
          _stepReset = true;
          _infoText =
              'Nếu email tồn tại trên hệ thống, hướng dẫn và mã đặt lại mật khẩu đã được gửi tới hộp thư của bạn.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() => _errorText = OnlineAccountService.formatAccountErrorVi(e));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _submitResetPassword() async {
    setState(() {
      _tokenError = _tokenController.text.trim().isEmpty
          ? 'Vui lòng nhập mã đặt lại mật khẩu.'
          : null;
      _newPassError = !ViStrings.isValidPassword(_newPasswordController.text)
          ? 'Mật khẩu mới cần tối thiểu 8 ký tự.'
          : null;
      _errorText = null;
    });
    if (_tokenError != null || _newPassError != null) return;

    setState(() => _loading = true);
    try {
      await ref.read(onlineAccountServiceProvider).resetPassword(
            token: _tokenController.text.trim(),
            newPassword: _newPasswordController.text,
          );
      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (mounted) {
        setState(() => _errorText = OnlineAccountService.formatAccountErrorVi(e));
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final LifeOsTokens tokens = LifeOsTokens.of(context);
    final double bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    return Material(
      color: tokens.bgSurface,
      borderRadius: const BorderRadius.vertical(
        top: Radius.circular(LifeOsTokens.radiusLg),
      ),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: EdgeInsets.fromLTRB(20, 16, 20, 20 + bottomInset),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: <Widget>[
                Expanded(
                  child: Text(
                    'Quên mật khẩu',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                TextButton(
                  onPressed: _loading
                      ? null
                      : () => setState(() => _stepReset = !_stepReset),
                  child: Text(_stepReset ? 'Gửi lại email' : 'Đã có mã đặt lại?'),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (_errorText != null) ...<Widget>[
              LifeOsErrorBanner(message: _errorText!),
              const SizedBox(height: 10),
            ],
            if (_infoText != null) ...<Widget>[
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: tokens.info.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(LifeOsTokens.radiusMd),
                ),
                child: Text(
                  _infoText!,
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w600,
                    color: tokens.info,
                  ),
                ),
              ),
              const SizedBox(height: 10),
            ],
            if (!_stepReset) ...<Widget>[
              TextField(
                key: const Key('input_forgot_email'),
                controller: _emailController,
                focusNode: _emailFocus,
                keyboardType: TextInputType.emailAddress,
                decoration: InputDecoration(
                  labelText: 'Email đã đăng ký *',
                  hintText: 'ban@example.com',
                  errorText: _emailError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 14),
              FilledButton(
                key: const Key('btn_send_forgot_email'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _loading ? null : _requestResetEmail,
                child: _loading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Gửi mã đặt lại mật khẩu'),
              ),
            ] else ...<Widget>[
              TextField(
                key: const Key('input_reset_token'),
                controller: _tokenController,
                focusNode: _tokenFocus,
                decoration: InputDecoration(
                  labelText: 'Mã đặt lại từ email *',
                  errorText: _tokenError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                key: const Key('input_reset_new_password'),
                controller: _newPasswordController,
                focusNode: _newPassFocus,
                obscureText: true,
                decoration: InputDecoration(
                  labelText: 'Mật khẩu mới (tối thiểu 8 ký tự) *',
                  errorText: _newPassError,
                  border: const OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 14),
              FilledButton(
                key: const Key('btn_submit_reset_password'),
                style: FilledButton.styleFrom(
                  minimumSize: const Size.fromHeight(LifeOsTokens.minTouchTarget),
                ),
                onPressed: _loading ? null : _submitResetPassword,
                child: _loading
                    ? const SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Text('Xác nhận đổi mật khẩu'),
              ),
            ],
          ],
        ),
      ),
      ),
    );
  }
}
