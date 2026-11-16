import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/l10n/vi_strings.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_tokens.dart';
import '../../../core/widgets/lifeos_widgets.dart';
import '../data/online_account_service.dart';

/// Hộp thoại Đổi mật khẩu trong Cài đặt (`POST /api/v1/me/change-password`).
class ChangePasswordDialog extends ConsumerStatefulWidget {
  const ChangePasswordDialog({super.key});

  @override
  ConsumerState<ChangePasswordDialog> createState() =>
      _ChangePasswordDialogState();
}

class _ChangePasswordDialogState extends ConsumerState<ChangePasswordDialog> {
  final TextEditingController _currentController = TextEditingController();
  final TextEditingController _newController = TextEditingController();
  final TextEditingController _confirmController = TextEditingController();
  final FocusNode _currentFocus = FocusNode();
  final FocusNode _newFocus = FocusNode();
  final FocusNode _confirmFocus = FocusNode();

  bool _loading = false;
  String? _currentError;
  String? _newError;
  String? _confirmError;
  String? _serverError;

  @override
  void initState() {
    super.initState();
    _currentFocus.addListener(() {
      if (!_currentFocus.hasFocus && mounted) {
        setState(() {
          _currentError = _currentController.text.isEmpty
              ? 'Vui lòng nhập mật khẩu hiện tại.'
              : null;
        });
      }
    });
    _newFocus.addListener(() {
      if (!_newFocus.hasFocus && mounted) {
        setState(() {
          _newError = !ViStrings.isValidPassword(_newController.text)
              ? 'Mật khẩu mới cần tối thiểu 8 ký tự.'
              : null;
        });
      }
    });
    _confirmFocus.addListener(() {
      if (!_confirmFocus.hasFocus && mounted) {
        setState(() {
          _confirmError = _confirmController.text != _newController.text
              ? 'Mật khẩu xác nhận không khớp.'
              : null;
        });
      }
    });
  }

  @override
  void dispose() {
    _currentController.dispose();
    _newController.dispose();
    _confirmController.dispose();
    _currentFocus.dispose();
    _newFocus.dispose();
    _confirmFocus.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _currentError = _currentController.text.isEmpty
          ? 'Vui lòng nhập mật khẩu hiện tại.'
          : null;
      _newError = !ViStrings.isValidPassword(_newController.text)
          ? 'Mật khẩu mới cần tối thiểu 8 ký tự.'
          : null;
      _confirmError = _confirmController.text != _newController.text
          ? 'Mật khẩu xác nhận không khớp.'
          : null;
      _serverError = null;
    });
    if (_currentError != null || _newError != null || _confirmError != null) {
      return;
    }

    setState(() => _loading = true);
    try {
      await ref.read(onlineAccountServiceProvider).changePassword(
            currentPassword: _currentController.text,
            newPassword: _newController.text,
          );
      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _serverError = OnlineAccountService.formatAccountErrorVi(e);
        });
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Đổi mật khẩu tài khoản'),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: <Widget>[
            if (_serverError != null) ...<Widget>[
              LifeOsErrorBanner(message: _serverError!),
              const SizedBox(height: 10),
            ],
            TextField(
              key: const Key('input_change_current_password'),
              controller: _currentController,
              focusNode: _currentFocus,
              obscureText: true,
              decoration: InputDecoration(
                labelText: 'Mật khẩu hiện tại *',
                errorText: _currentError,
                border: const OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('input_change_new_password'),
              controller: _newController,
              focusNode: _newFocus,
              obscureText: true,
              decoration: InputDecoration(
                labelText: 'Mật khẩu mới (tối thiểu 8 ký tự) *',
                errorText: _newError,
                border: const OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('input_change_confirm_password'),
              controller: _confirmController,
              focusNode: _confirmFocus,
              obscureText: true,
              decoration: InputDecoration(
                labelText: 'Nhập lại mật khẩu mới *',
                errorText: _confirmError,
                border: const OutlineInputBorder(),
              ),
            ),
          ],
        ),
      ),
      actions: <Widget>[
        TextButton(
          onPressed: _loading ? null : () => Navigator.of(context).pop(false),
          child: const Text(ViStrings.cancelAction),
        ),
        FilledButton(
          key: const Key('btn_submit_change_password'),
          style: FilledButton.styleFrom(
            minimumSize: const Size(120, LifeOsTokens.minTouchTarget),
          ),
          onPressed: _loading ? null : _submit,
          child: _loading
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Cập nhật mật khẩu'),
        ),
      ],
    );
  }
}

