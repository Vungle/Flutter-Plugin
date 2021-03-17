import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vungle/vungle.dart';

void main() {
  const MethodChannel channel = MethodChannel('vungle');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return 'Accepted';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getContentStatus', () async {
    expect(await Vungle.getConsentStatus(), UserConsentStatus.Accepted);
  });
}
