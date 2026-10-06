import 'dart:async';
import 'package:flutter/material.dart';

void main() {
  runApp(const VoltEliteApp());
}

// ============================================================================
// 1. DESIGN SYSTEM & THEME
// ============================================================================
class VoltColors {
  static const Color darkBg = Color(0xFF08060D);
  static const Color surface = Color(0xFF130F26);
  static const Color card = Color(0xFF1B1636);
  static const Color cardElevated = Color(0xFF261F48);
  static const Color cardBorder = Color(0xFF2E2756);

  static const Color green = Color(0xFF4AE176);
  static const Color cyan = Color(0xFF55DFFF);
  static const Color purple = Color(0xFFDDB7FF);
  static const Color red = Color(0xFFFF5252);

  static const Color textPrimary = Color(0xFFF3F0FA);
  static const Color textSecondary = Color(0xFFAFA7C9);
  static const Color textMuted = Color(0xFF756E94);
}

// ============================================================================
// 2. DATA MODELS (DART)
// ============================================================================
enum ChargingNetwork {
  tataPower('Tata Power', Color(0xFF0072CE)),
  chargeZone('ChargeZone', Color(0xFF00B0FF)),
  statiq('Statiq', Color(0xFFFF6D00)),
  jioBp('Jio-bp', Color(0xFF00897B)),
  zeon('Zeon Charging', Color(0xFF7C4DFF)),
  bpcl('BPCL eDrive', Color(0xFFFFD600)),
  kazam('Kazam', Color(0xFFE040FB)),
  atherGrid('Ather Grid', Color(0xFF00E676));

  final String displayName;
  final Color brandColor;
  const ChargingNetwork(this.displayName, this.brandColor);
}

enum ConnectorType {
  ccs2('CCS 2', true),
  type2('Type 2', false),
  chademo('CHAdeMO', true),
  acType1('AC 7.4kW', false);

  final String displayName;
  final bool isFast;
  const ConnectorType(this.displayName, this.isFast);
}

enum PaymentMethod {
  upi('UPI Transfer', 'Google Pay, PhonePe, Paytm, BHIM'),
  wallet('VoltElite Wallet', 'Available Balance: ₹1,500.00'),
  card('Credit / Debit Card', 'Visa ending in 4821'),
  preAuth('Pre-Authorization Hold', '₹500 temporary hold released on completion');

  final String title;
  final String subtitle;
  const PaymentMethod(this.title, this.subtitle);
}

class EvVehicle {
  final String make;
  final String model;
  final double batteryCapacityKwh;
  final int realWorldRangeKm;
  final ConnectorType connectorType;
  final int currentBatteryPercent;

  const EvVehicle({
    required this.make,
    required this.model,
    required this.batteryCapacityKwh,
    required this.realWorldRangeKm,
    required this.connectorType,
    this.currentBatteryPercent = 68,
  });
}

class ParsedQrCharger {
  final ChargingNetwork provider;
  final String stationId;
  final String stationName;
  final String evseId;
  final ConnectorType connectorType;
  final int powerKw;
  final double tariffPerKwh;
  final String location;
  final bool supportsRemoteStart;
  final String rawPayload;

  const ParsedQrCharger({
    required this.provider,
    required this.stationId,
    required this.stationName,
    required this.evseId,
    required this.connectorType,
    required this.powerKw,
    required this.tariffPerKwh,
    required this.location,
    required this.supportsRemoteStart,
    required this.rawPayload,
  });
}

class ChargingReceipt {
  final String sessionId;
  final String txnId;
  final String stationName;
  final String evseId;
  final ChargingNetwork network;
  final ConnectorType connectorType;
  final int powerKw;
  final double energyDeliveredKwh;
  final int chargingTimeMinutes;
  final double totalAmountRupees;
  final double tariffPerKwh;
  final String paymentMethod;
  final String timestampFormatted;
  final String invoiceNumber;

  const ChargingReceipt({
    required this.sessionId,
    required this.txnId,
    required this.stationName,
    required this.evseId,
    required this.network,
    required this.connectorType,
    required this.powerKw,
    required this.energyDeliveredKwh,
    required this.chargingTimeMinutes,
    required this.totalAmountRupees,
    required this.tariffPerKwh,
    required this.paymentMethod,
    required this.timestampFormatted,
    required this.invoiceNumber,
  });
}

// ============================================================================
// 3. QR CHARGING ENGINE (PARSER & VERIFICATION)
// ============================================================================
class QRChargingService {
  ParsedQrCharger? parseQr(String raw) {
    final trimmed = raw.trim();
    if (trimmed.isEmpty) return null;

    if (trimmed.contains('INCOMPATIBLE') || trimmed.contains('AC-TYPE2')) {
      return ParsedQrCharger(
        provider: ChargingNetwork.kazam,
        stationId: 'kz_01',
        stationName: 'Kazam EV Hub — Bhawarkuan',
        evseId: 'EVSE-KZ-02',
        connectorType: ConnectorType.type2,
        powerKw: 11,
        tariffPerKwh: 16.0,
        location: 'Bhawarkuan Square, Indore',
        supportsRemoteStart: true,
        rawPayload: trimmed,
      );
    }

    if (trimmed.contains('chargezone') || trimmed.contains('CZ-PHX')) {
      return ParsedQrCharger(
        provider: ChargingNetwork.chargeZone,
        stationId: 'cz_01',
        stationName: 'ChargeZone — Phoenix Citadel Mall',
        evseId: 'EVSE-CZ-120A',
        connectorType: ConnectorType.ccs2,
        powerKw: 120,
        tariffPerKwh: 21.0,
        location: 'Basement B2, Phoenix Citadel, Indore',
        supportsRemoteStart: true,
        rawPayload: trimmed,
      );
    }

    if (trimmed.contains('statiq') || trimmed.contains('STATIQ-PAL')) {
      return ParsedQrCharger(
        provider: ChargingNetwork.statiq,
        stationId: 'stq_01',
        stationName: 'Statiq EV Station — Palasia',
        evseId: 'EVSE-STQ-50A',
        connectorType: ConnectorType.ccs2,
        powerKw: 50,
        tariffPerKwh: 17.5,
        location: 'Old Palasia, AB Road, Indore',
        supportsRemoteStart: true,
        rawPayload: trimmed,
      );
    }

    if (trimmed.contains('jiobp') || trimmed.contains('JIOBP-SUPER')) {
      return ParsedQrCharger(
        provider: ChargingNetwork.jioBp,
        stationId: 'jbp_01',
        stationName: 'Jio-bp pulse Station — Super Corridor',
        evseId: 'EVSE-JBP-60A',
        connectorType: ConnectorType.ccs2,
        powerKw: 60,
        tariffPerKwh: 19.0,
        location: 'Super Corridor Airport Rd, TCS Square, Indore',
        supportsRemoteStart: true,
        rawPayload: trimmed,
      );
    }

    // Default Tata Power EVSE-08
    return ParsedQrCharger(
      provider: ChargingNetwork.tataPower,
      stationId: 'tp_01',
      stationName: 'Tata Power Charging Station',
      evseId: 'EVSE-08',
      connectorType: ConnectorType.ccs2,
      powerKw: 60,
      tariffPerKwh: 18.5,
      location: 'Near Vijay Nagar Square, AB Road, Indore',
      supportsRemoteStart: true,
      rawPayload: trimmed,
    );
  }

  bool isCompatible(EvVehicle vehicle, ParsedQrCharger charger) {
    return vehicle.connectorType == charger.connectorType;
  }
}

// ============================================================================
// 4. GLOBAL STATE (INHERITED NOTIFIER)
// ============================================================================
class VoltState extends ChangeNotifier {
  final QRChargingService qrService = QRChargingService();

  final EvVehicle vehicle = const EvVehicle(
    make: 'Tata',
    model: 'Nexon EV',
    batteryCapacityKwh: 40.5,
    realWorldRangeKm: 312,
    connectorType: ConnectorType.ccs2,
    currentBatteryPercent: 68,
  );

  ParsedQrCharger? scannedCharger;
  PaymentMethod selectedPayment = PaymentMethod.upi;
  ChargingReceipt? latestReceipt;

  final List<ChargingReceipt> history = [
    const ChargingReceipt(
      sessionId: 'SESS-1001',
      txnId: 'TXN-839201',
      stationName: 'Tata Power Charging Station',
      evseId: 'EVSE-08',
      network: ChargingNetwork.tataPower,
      connectorType: ConnectorType.ccs2,
      powerKw: 60,
      energyDeliveredKwh: 31.6,
      chargingTimeMinutes: 32,
      totalAmountRupees: 412.0,
      tariffPerKwh: 18.5,
      paymentMethod: 'UPI (Google Pay)',
      timestampFormatted: '02 Oct 2026, 05:40 PM',
      invoiceNumber: 'INV-839201',
    ),
    const ChargingReceipt(
      sessionId: 'SESS-1002',
      txnId: 'TXN-729104',
      stationName: 'ChargeZone — Phoenix Citadel Mall',
      evseId: 'EVSE-CZ-01',
      network: ChargingNetwork.chargeZone,
      connectorType: ConnectorType.ccs2,
      powerKw: 120,
      energyDeliveredKwh: 42.0,
      chargingTimeMinutes: 24,
      totalAmountRupees: 882.0,
      tariffPerKwh: 21.0,
      paymentMethod: 'VoltElite Wallet',
      timestampFormatted: '01 Oct 2026, 02:15 PM',
      invoiceNumber: 'INV-729104',
    ),
  ];

  // Active Charging Session Simulation
  bool isCharging = false;
  double currentPercent = 68.0;
  double energyDelivered = 0.5;
  double currentCost = 10.0;
  int estTimeLeftMin = 22;
  Timer? _timer;

  bool verifyCharger(String payload) {
    final parsed = qrService.parseQr(payload);
    if (parsed != null) {
      scannedCharger = parsed;
      notifyListeners();
      return true;
    }
    return false;
  }

  void selectPaymentMethod(PaymentMethod method) {
    selectedPayment = method;
    notifyListeners();
  }

  void startCharging() {
    if (scannedCharger == null) return;
    isCharging = true;
    currentPercent = 68.0;
    energyDelivered = 0.5;
    currentCost = scannedCharger!.tariffPerKwh * 0.5;
    estTimeLeftMin = 22;
    notifyListeners();

    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 2), (t) {
      if (currentPercent >= 85.0) {
        stopCharging();
      } else {
        currentPercent += 1.0;
        energyDelivered += 0.35;
        currentCost = energyDelivered * (scannedCharger?.tariffPerKwh ?? 18.5);
        if (estTimeLeftMin > 1) estTimeLeftMin--;
        notifyListeners();
      }
    });
  }

  void stopCharging() {
    _timer?.cancel();
    isCharging = false;

    if (scannedCharger != null) {
      final receipt = ChargingReceipt(
        sessionId: 'SESS-${DateTime.now().millisecondsSinceEpoch}',
        txnId: 'TXN-${100000 + DateTime.now().millisecond}',
        stationName: scannedCharger!.stationName,
        evseId: scannedCharger!.evseId,
        network: scannedCharger!.provider,
        connectorType: scannedCharger!.connectorType,
        powerKw: scannedCharger!.powerKw,
        energyDeliveredKwh: double.parse(energyDelivered.toStringAsFixed(1)),
        chargingTimeMinutes: 32,
        totalAmountRupees: double.parse(currentCost.toStringAsFixed(0)),
        tariffPerKwh: scannedCharger!.tariffPerKwh,
        paymentMethod: selectedPayment.title,
        timestampFormatted: '03 Oct 2026, 06:35 PM',
        invoiceNumber: 'INV-${DateTime.now().microsecondsSinceEpoch.toString().substring(8)}',
      );
      latestReceipt = receipt;
      history.insert(0, receipt);
    }
    notifyListeners();
  }
}

class VoltScope extends InheritedNotifier<VoltState> {
  const VoltScope({super.key, required VoltState state, required super.child})
      : super(notifier: state);

  static VoltState of(BuildContext context) {
    return context.dependOnInheritedWidgetOfExactType<VoltScope>()!.notifier!;
  }
}

// ============================================================================
// 5. MAIN APPLICATION
// ============================================================================
class VoltEliteApp extends StatefulWidget {
  const VoltEliteApp({super.key});

  @override
  State<VoltEliteApp> createState() => _VoltEliteAppState();
}

class _VoltEliteAppState extends State<VoltEliteApp> {
  final VoltState _state = VoltState();

  @override
  Widget build(BuildContext context) {
    return VoltScope(
      state: _state,
      child: MaterialApp(
        title: 'VoltElite',
        debugShowCheckedModeBanner: false,
        theme: ThemeData(
          brightness: Brightness.dark,
          scaffoldBackgroundColor: VoltColors.darkBg,
          primaryColor: VoltColors.green,
          cardColor: VoltColors.card,
          colorScheme: const ColorScheme.dark(
            primary: VoltColors.green,
            secondary: VoltColors.cyan,
            surface: VoltColors.surface,
          ),
          fontFamily: 'sans-serif',
        ),
        home: const SplashScreen(),
      ),
    );
  }
}

// ============================================================================
// 6. SPLASH SCREEN
// ============================================================================
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> with SingleTickerProviderStateMixin {
  late AnimationController _anim;

  @override
  void initState() {
    super.initState();
    _anim = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400))
      ..repeat(reverse: true);

    Timer(const Duration(seconds: 2), () {
      if (mounted) {
        Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const MainTabsScreen()));
      }
    });
  }

  @override
  void dispose() {
    _anim.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            AnimatedBuilder(
              animation: _anim,
              builder: (context, child) {
                return Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: VoltColors.green.withOpacity(0.15 + (_anim.value * 0.15)),
                    border: Border.all(color: VoltColors.green, width: 2),
                  ),
                  child: const Icon(Icons.bolt, size: 50, color: VoltColors.green),
                );
              },
            ),
            const SizedBox(height: 20),
            const Text('VoltElite', style: TextStyle(fontSize: 36, fontWeight: FontWeight.bold, letterSpacing: 1.2)),
            const SizedBox(height: 6),
            const Text('Charge • Explore • Go Further', style: TextStyle(color: VoltColors.cyan, fontSize: 14)),
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
              decoration: BoxDecoration(
                color: VoltColors.card,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: VoltColors.green.withOpacity(0.4)),
              ),
              child: const Text('⚡ OCPI 2.2 ROAMING ENABLED', style: TextStyle(color: VoltColors.green, fontSize: 11, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }
}

// ============================================================================
// 7. BOTTOM NAVIGATION
// ============================================================================
class MainTabsScreen extends StatefulWidget {
  const MainTabsScreen({super.key});

  @override
  State<MainTabsScreen> createState() => _MainTabsScreenState();
}

class _MainTabsScreenState extends State<MainTabsScreen> {
  int _currentIndex = 0;
  final List<Widget> _pages = const [
    HomeScreen(),
    MapCanvasScreen(),
    TripEstimatorScreen(),
    ChargingHistoryScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: _pages[_currentIndex],
      bottomNavigationBar: NavigationBar(
        backgroundColor: VoltColors.card,
        selectedIndex: _currentIndex,
        indicatorColor: VoltColors.green.withOpacity(0.2),
        onDestinationSelected: (i) => setState(() => _currentIndex = i),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home, color: VoltColors.green), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.map_outlined), selectedIcon: Icon(Icons.map, color: VoltColors.green), label: 'Map'),
          NavigationDestination(icon: Icon(Icons.calculate_outlined), selectedIcon: Icon(Icons.calculate, color: VoltColors.green), label: 'Estimator'),
          NavigationDestination(icon: Icon(Icons.history_outlined), selectedIcon: Icon(Icons.history, color: VoltColors.green), label: 'History'),
        ],
      ),
    );
  }
}

// ============================================================================
// 8. HOME SCREEN
// ============================================================================
class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final volt = VoltScope.of(context);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
          decoration: BoxDecoration(
            color: VoltColors.card,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: VoltColors.cardBorder),
          ),
          child: const Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.location_on, color: VoltColors.green, size: 16),
              SizedBox(width: 6),
              Text('Indore, MP', style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold)),
              Icon(Icons.keyboard_arrow_down, color: VoltColors.textSecondary, size: 18),
            ],
          ),
        ),
        actions: [
          IconButton(
            icon: Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(shape: BoxShape.circle, color: VoltColors.green.withOpacity(0.15), border: Border.all(color: VoltColors.green)),
              child: const Icon(Icons.qr_code_scanner, color: VoltColors.green, size: 18),
            ),
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const QRScannerScreen())),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: VoltColors.card,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: VoltColors.cardBorder),
            ),
            child: Row(
              children: [
                const Icon(Icons.directions_car, color: VoltColors.cyan, size: 28),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('${volt.vehicle.make} ${volt.vehicle.model}', style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                      Text('${volt.vehicle.connectorType.displayName} • ${volt.vehicle.realWorldRangeKm} km real range', style: const TextStyle(color: VoltColors.textSecondary, fontSize: 12)),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(color: VoltColors.green.withOpacity(0.2), borderRadius: BorderRadius.circular(8)),
                  child: Text('${volt.vehicle.currentBatteryPercent}% 🔋', style: const TextStyle(color: VoltColors.green, fontWeight: FontWeight.bold)),
                ),
              ],
            ),
          ),
          const SizedBox(height: 20),

          // Scan QR to Charge Hero
          InkWell(
            onTap: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const QRScannerScreen())),
            borderRadius: BorderRadius.circular(20),
            child: Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: VoltColors.card,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: VoltColors.green.withOpacity(0.6)),
                gradient: LinearGradient(
                  colors: [VoltColors.green.withOpacity(0.2), Colors.transparent],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
              ),
              child: Row(
                children: [
                  Container(
                    width: 54,
                    height: 54,
                    decoration: BoxDecoration(shape: BoxShape.circle, color: VoltColors.green, boxShadow: [BoxShadow(color: VoltColors.green.withOpacity(0.4), blurRadius: 12)]),
                    child: const Icon(Icons.qr_code_scanner, color: VoltColors.darkBg, size: 30),
                  ),
                  const SizedBox(width: 16),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text('Scan QR to Charge', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                        SizedBox(height: 4),
                        Text('Reach charger → Scan QR → Start charging instantly', style: TextStyle(color: VoltColors.textSecondary, fontSize: 12)),
                      ],
                    ),
                  ),
                  const Icon(Icons.arrow_forward_ios, color: VoltColors.green, size: 16),
                ],
              ),
            ),
          ),

          const SizedBox(height: 24),
          const Text('Nearby EV Chargers', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),

          _stationItem(context, 'Tata Power Charging Station', 'Near Vijay Nagar, Indore', '60 kW DC', 'CCS 2', '₹18.50', 'VOLT-DEMO-STN001-EVSE08-CCS2'),
          _stationItem(context, 'ChargeZone — Phoenix Citadel', 'Basement B2, Phoenix Mall', '120 kW Ultra-Fast', 'CCS 2', '₹21.00', 'CZ-PHX-120KW-CCS2'),
          _stationItem(context, 'Statiq EV Station — Palasia', 'Old Palasia, AB Road', '50 kW DC', 'CCS 2', '₹17.50', 'STATIQ-PAL-50KW-CCS2'),
        ],
      ),
    );
  }

  Widget _stationItem(BuildContext context, String name, String loc, String power, String conn, String price, String mockPayload) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: VoltColors.card,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: VoltColors.cardBorder),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(color: VoltColors.green.withOpacity(0.2), borderRadius: BorderRadius.circular(6)),
                child: const Text('Available', style: TextStyle(color: VoltColors.green, fontSize: 11, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(loc, style: const TextStyle(color: VoltColors.textSecondary, fontSize: 12)),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('$power • $conn • $price/kWh', style: const TextStyle(color: VoltColors.cyan, fontSize: 13, fontWeight: FontWeight.w600)),
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(backgroundColor: VoltColors.green, foregroundColor: VoltColors.darkBg),
                onPressed: () {
                  VoltScope.of(context).verifyCharger(mockPayload);
                  Navigator.push(context, MaterialPageRoute(builder: (_) => const ChargerVerificationScreen()));
                },
                icon: const Icon(Icons.bolt, size: 16),
                label: const Text('Charge'),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

// ============================================================================
// 9. QR SCANNER SCREEN
// ============================================================================
class QRScannerScreen extends StatefulWidget {
  const QRScannerScreen({super.key});

  @override
  State<QRScannerScreen> createState() => _QRScannerScreenState();
}

class _QRScannerScreenState extends State<QRScannerScreen> with SingleTickerProviderStateMixin {
  late AnimationController _laserController;
  final TextEditingController _manualInput = TextEditingController();
  bool _isTorchOn = false;

  @override
  void initState() {
    super.initState();
    _laserController = AnimationController(vsync: this, duration: const Duration(milliseconds: 1800))..repeat(reverse: true);
  }

  @override
  void dispose() {
    _laserController.dispose();
    _manualInput.dispose();
    super.dispose();
  }

  void _submitScan(String payload) {
    final volt = VoltScope.of(context);
    final ok = volt.verifyCharger(payload);
    if (ok) {
      Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const ChargerVerificationScreen()));
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Invalid QR code. Please scan a certified EVSE code.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Scan QR to Charge'),
        backgroundColor: Colors.transparent,
        actions: [
          IconButton(
            icon: Icon(_isTorchOn ? Icons.flash_on : Icons.flash_off, color: _isTorchOn ? VoltColors.green : Colors.white),
            onPressed: () => setState(() => _isTorchOn = !_isTorchOn),
          ),
        ],
      ),
      body: Column(
        children: [
          const SizedBox(height: 12),
          const Text('Align camera with the QR code on the EVSE or connector', style: TextStyle(color: VoltColors.textSecondary, fontSize: 13)),
          const SizedBox(height: 24),

          Center(
            child: SizedBox(
              width: 260,
              height: 260,
              child: Stack(
                children: [
                  Container(
                    decoration: BoxDecoration(
                      color: VoltColors.surface,
                      borderRadius: BorderRadius.circular(24),
                      border: Border.all(color: VoltColors.cardBorder, width: 2),
                    ),
                  ),
                  AnimatedBuilder(
                    animation: _laserController,
                    builder: (context, _) {
                      return CustomPaint(
                        size: const Size(260, 260),
                        painter: LaserScanPainter(_laserController.value),
                      );
                    },
                  ),
                  const Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.qr_code_scanner, size: 54, color: VoltColors.cyan),
                        SizedBox(height: 8),
                        Text('Scanning...', style: TextStyle(color: VoltColors.textSecondary, fontSize: 12)),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ),

          const SizedBox(height: 24),

          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('⚡ Quick Demo Scans (1-Tap Simulation):', style: TextStyle(color: VoltColors.cyan, fontWeight: FontWeight.bold, fontSize: 13)),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    _chip('Tata Power (60kW CCS2)', 'VOLT-DEMO-STN001-EVSE08-CCS2'),
                    _chip('ChargeZone (120kW Fast)', 'CZ-PHX-120KW-CCS2'),
                    _chip('Statiq (50kW Palasia)', 'STATIQ-PAL-50KW-CCS2'),
                    _chip('Incompatible (Type 2 AC)', 'INCOMPATIBLE-AC-TYPE2'),
                  ],
                ),
              ],
            ),
          ),

          const Spacer(),

          Container(
            padding: const EdgeInsets.all(20),
            color: VoltColors.card,
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _manualInput,
                    decoration: InputDecoration(
                      hintText: 'Enter Charger ID (e.g. EVSE-08)',
                      hintStyle: const TextStyle(color: VoltColors.textMuted, fontSize: 13),
                      filled: true,
                      fillColor: VoltColors.cardElevated,
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(backgroundColor: VoltColors.green, foregroundColor: VoltColors.darkBg, shape: RoundedCornerShape(12), padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14)),
                  onPressed: () {
                    if (_manualInput.text.isNotEmpty) _submitScan(_manualInput.text);
                  },
                  child: const Text('Verify', style: TextStyle(fontWeight: FontWeight.bold)),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _chip(String label, String payload) {
    return ActionChip(
      backgroundColor: VoltColors.card,
      side: const BorderSide(color: VoltColors.cardBorder),
      label: Text(label, style: const TextStyle(fontSize: 12, color: VoltColors.textPrimary)),
      onPressed: () => _submitScan(payload),
    );
  }
}

class LaserScanPainter extends CustomPainter {
  final double progress;
  LaserScanPainter(this.progress);

  @override
  void paint(Canvas canvas, Size size) {
    final strokePaint = Paint()
      ..color = VoltColors.green
      ..style = PaintingStyle.stroke
      ..strokeWidth = 5;

    const cornerLen = 32.0;

    canvas.drawLine(const Offset(12, 12), const Offset(12 + cornerLen, 12), strokePaint);
    canvas.drawLine(const Offset(12, 12), const Offset(12, 12 + cornerLen), strokePaint);

    canvas.drawLine(Offset(size.width - 12, 12), Offset(size.width - 12 - cornerLen, 12), strokePaint);
    canvas.drawLine(Offset(size.width - 12, 12), Offset(size.width - 12, 12 + cornerLen), strokePaint);

    canvas.drawLine(Offset(12, size.height - 12), Offset(12 + cornerLen, size.height - 12), strokePaint);
    canvas.drawLine(Offset(12, size.height - 12), Offset(12, size.height - 12 - cornerLen), strokePaint);

    canvas.drawLine(Offset(size.width - 12, size.height - 12), Offset(size.width - 12 - cornerLen, size.height - 12), strokePaint);
    canvas.drawLine(Offset(size.width - 12, size.height - 12), Offset(size.width - 12, size.height - 12 - cornerLen), strokePaint);

    final y = 20 + (size.height - 40) * progress;
    final laserPaint = Paint()
      ..shader = LinearGradient(
        colors: [Colors.transparent, VoltColors.green, VoltColors.cyan, VoltColors.green, Colors.transparent],
      ).createShader(Rect.fromLTWH(0, y, size.width, 3))
      ..strokeWidth = 3;

    canvas.drawLine(Offset(20, y), Offset(size.width - 20, y), laserPaint);
  }

  @override
  bool shouldRepaint(covariant LaserScanPainter oldDelegate) => oldDelegate.progress != progress;
}

// ============================================================================
// 10. CHARGER VERIFICATION SCREEN
// ============================================================================
class ChargerVerificationScreen extends StatelessWidget {
  const ChargerVerificationScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final volt = VoltScope.of(context);
    final charger = volt.scannedCharger;
    if (charger == null) return const Scaffold(body: Center(child: Text('No charger verified.')));

    final isCompatible = volt.qrService.isCompatible(volt.vehicle, charger);

    return Scaffold(
      appBar: AppBar(title: const Text('Charger Found'), backgroundColor: Colors.transparent),
      body: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: VoltColors.card,
              borderRadius: BorderRadius.circular(20),
              border: Border.all(color: VoltColors.green.withOpacity(0.5)),
            ),
            child: Column(
              children: [
                const Icon(Icons.check_circle, color: VoltColors.green, size: 40),
                const SizedBox(height: 6),
                const Text('CHARGER VERIFIED', style: TextStyle(color: VoltColors.green, fontWeight: FontWeight.bold, letterSpacing: 0.8)),
                const SizedBox(height: 10),
                Text(charger.stationName, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold), textAlign: TextAlign.center),
                const SizedBox(height: 4),
                Text(charger.location, style: const TextStyle(color: VoltColors.textSecondary, fontSize: 12), textAlign: TextAlign.center),
                const Divider(height: 24, color: VoltColors.cardBorder),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    _specCol('POWER', '${charger.powerKw} kW', VoltColors.cyan),
                    _specCol('CONNECTOR', charger.connectorType.displayName, VoltColors.textPrimary),
                    _specCol('TARIFF', '₹${charger.tariffPerKwh}', VoltColors.green),
                  ],
                ),
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(color: VoltColors.cardElevated, borderRadius: BorderRadius.circular(8)),
                  child: Text('Charger ID: ${charger.evseId}', style: const TextStyle(color: VoltColors.textSecondary, fontSize: 12)),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: isCompatible ? VoltColors.card : VoltColors.red.withOpacity(0.15),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: isCompatible ? VoltColors.green.withOpacity(0.4) : VoltColors.red),
            ),
            child: Row(
              children: [
                Icon(isCompatible ? Icons.check_circle : Icons.warning, color: isCompatible ? VoltColors.green : VoltColors.red),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    isCompatible
                        ? 'Compatible with your ${volt.vehicle.make} ${volt.vehicle.model}'
                        : 'Connector mismatch: Charger provides ${charger.connectorType.displayName}, vehicle requires ${volt.vehicle.connectorType.displayName}.',
                    style: TextStyle(color: isCompatible ? VoltColors.green : VoltColors.red, fontSize: 13),
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 20),
          const Text('Select Payment Method', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          const SizedBox(height: 10),

          ...PaymentMethod.values.map((method) {
            final isSelected = volt.selectedPayment == method;
            return Container(
              margin: const EdgeInsets.only(bottom: 8),
              decoration: BoxDecoration(
                color: isSelected ? VoltColors.cardElevated : VoltColors.card,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: isSelected ? VoltColors.green : VoltColors.cardBorder),
              ),
              child: ListTile(
                leading: Icon(Icons.payment, color: isSelected ? VoltColors.green : VoltColors.textSecondary),
                title: Text(method.title, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600)),
                subtitle: Text(method.subtitle, style: const TextStyle(fontSize: 11, color: VoltColors.textSecondary)),
                trailing: isSelected ? const Icon(Icons.check_circle, color: VoltColors.green) : null,
                onTap: () => volt.selectPaymentMethod(method),
              ),
            );
          }),
        ],
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: isCompatible ? VoltColors.green : VoltColors.card,
              foregroundColor: VoltColors.darkBg,
              minimumSize: const Size.fromHeight(52),
              shape: RoundedCornerShape(14),
            ),
            onPressed: isCompatible
                ? () {
                    volt.startCharging();
                    Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const LiveChargingScreen()));
                  }
                : null,
            child: Text(isCompatible ? 'Confirm & Start Charging' : 'Incompatible Charger', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
          ),
        ),
      ),
    );
  }

  Widget _specCol(String title, String val, Color c) {
    return Column(
      children: [
        Text(title, style: const TextStyle(fontSize: 10, color: VoltColors.textMuted, fontWeight: FontWeight.bold)),
        Text(val, style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: c)),
      ],
    );
  }
}

// ============================================================================
// 11. LIVE CHARGING SCREEN
// ============================================================================
class LiveChargingScreen extends StatelessWidget {
  const LiveChargingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final volt = VoltScope.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text('Charging in Progress'), backgroundColor: Colors.transparent),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Stack(
              alignment: Alignment.Center,
              children: [
                SizedBox(
                  width: 200,
                  height: 200,
                  child: CircularProgressIndicator(
                    value: volt.currentPercent / 100,
                    strokeWidth: 14,
                    color: VoltColors.green,
                    backgroundColor: VoltColors.cardBorder,
                  ),
                ),
                Column(
                  children: [
                    Text('${volt.currentPercent.toInt()}%', style: const TextStyle(fontSize: 44, fontWeight: FontWeight.bold)),
                    const Text('⚡ 60 kW Fast', style: TextStyle(color: VoltColors.cyan, fontWeight: FontWeight.bold)),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 36),
            Text('Energy Added: ${volt.energyDelivered.toStringAsFixed(1)} kWh', style: const TextStyle(fontSize: 18)),
            const SizedBox(height: 6),
            Text('Time Left: ${volt.estTimeLeftMin} mins to 85%', style: const TextStyle(color: VoltColors.textSecondary, fontSize: 13)),
            const SizedBox(height: 8),
            Text('Current Amount: ₹${volt.currentCost.toStringAsFixed(0)}', style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: VoltColors.green)),
            const SizedBox(height: 40),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(backgroundColor: VoltColors.red, foregroundColor: Colors.white, minimumSize: const Size(220, 52), shape: RoundedCornerShape(14)),
              onPressed: () {
                showDialog(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    backgroundColor: VoltColors.card,
                    title: const Text('Stop Charging Session?'),
                    content: Text('Current energy: ${volt.energyDelivered.toStringAsFixed(1)} kWh\nCurrent amount: ₹${volt.currentCost.toStringAsFixed(0)}'),
                    actions: [
                      TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
                      ElevatedButton(
                        style: ElevatedButton.styleFrom(backgroundColor: VoltColors.red),
                        onPressed: () {
                          Navigator.pop(ctx);
                          volt.stopCharging();
                          Navigator.pushReplacement(context, MaterialPageRoute(builder: (_) => const ChargingReceiptScreen()));
                        },
                        child: const Text('Stop Charging'),
                      ),
                    ],
                  ),
                );
              },
              icon: const Icon(Icons.stop),
              label: const Text('Stop Charging', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }
}

// ============================================================================
// 12. DIGITAL RECEIPT SCREEN
// ============================================================================
class ChargingReceiptScreen extends StatelessWidget {
  const ChargingReceiptScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final receipt = VoltScope.of(context).latestReceipt;
    if (receipt == null) return const Scaffold(body: Center(child: Text('Receipt not found')));

    return Scaffold(
      appBar: AppBar(title: const Text('Charging Receipt'), backgroundColor: Colors.transparent),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            const Icon(Icons.check_circle, color: VoltColors.green, size: 64),
            const SizedBox(height: 8),
            const Text('Charging Complete!', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
            const SizedBox(height: 4),
            const Text('Transaction settled successfully', style: TextStyle(color: VoltColors.cyan, fontSize: 13)),
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(color: VoltColors.card, borderRadius: BorderRadius.circular(18), border: Border.all(color: VoltColors.cardBorder)),
              child: Column(
                children: [
                  _row('Station', receipt.stationName),
                  _row('Charger / EVSE', receipt.evseId),
                  _row('Energy Delivered', '${receipt.energyDeliveredKwh} kWh'),
                  _row('Charging Duration', '${receipt.chargingTimeMinutes} mins'),
                  _row('Payment Method', receipt.paymentMethod),
                  _row('Transaction ID', receipt.txnId),
                  _row('Invoice Number', receipt.invoiceNumber),
                  const Divider(color: VoltColors.cardBorder, height: 24),
                  _row('Total Paid', '₹${receipt.totalAmountRupees.toInt()}', isBold: true),
                ],
              ),
            ),
            const Spacer(),
            ElevatedButton(
              style: ElevatedButton.styleFrom(backgroundColor: VoltColors.green, foregroundColor: VoltColors.darkBg, minimumSize: const Size.fromHeight(52), shape: RoundedCornerShape(14)),
              onPressed: () => Navigator.pushAndRemoveUntil(context, MaterialPageRoute(builder: (_) => const MainTabsScreen()), (r) => false),
              child: const Text('Done', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  Widget _row(String l, String v, {bool isBold = false}) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(l, style: const TextStyle(color: VoltColors.textSecondary, fontSize: 13)),
          Text(v, style: TextStyle(fontWeight: isBold ? FontWeight.bold : FontWeight.w600, fontSize: isBold ? 20 : 13, color: isBold ? VoltColors.green : VoltColors.textPrimary)),
        ],
      ),
    );
  }
}

// ============================================================================
// 13. CHARGING HISTORY SCREEN
// ============================================================================
class ChargingHistoryScreen extends StatelessWidget {
  const ChargingHistoryScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final history = VoltScope.of(context).history;

    return Scaffold(
      appBar: AppBar(title: const Text('Charging History'), backgroundColor: Colors.transparent),
      body: history.isEmpty
          ? const Center(child: Text('No charging history recorded.'))
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: history.length,
              itemBuilder: (context, i) {
                final item = history[i];
                return Container(
                  margin: const EdgeInsets.only(bottom: 12),
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(color: VoltColors.card, borderRadius: BorderRadius.circular(16), border: Border.all(color: VoltColors.cardBorder)),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(item.stationName, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                          Text('₹${item.totalAmountRupees.toInt()}', style: const TextStyle(color: VoltColors.green, fontWeight: FontWeight.bold, fontSize: 16)),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text('${item.network.displayName} • ${item.evseId} • ${item.connectorType.displayName}', style: const TextStyle(color: VoltColors.cyan, fontSize: 12)),
                      const SizedBox(height: 8),
                      Text('${item.energyDeliveredKwh} kWh in ${item.chargingTimeMinutes} mins • ${item.timestampFormatted}', style: const TextStyle(color: VoltColors.textSecondary, fontSize: 12)),
                    ],
                  ),
                );
              },
            ),
    );
  }
}

// ============================================================================
// 14. MAP CANVAS SCREEN
// ============================================================================
class MapCanvasScreen extends StatelessWidget {
  const MapCanvasScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Indore EV Charging Grid'), backgroundColor: Colors.transparent),
      body: Stack(
        children: [
          CustomPaint(
            size: Size.infinite,
            painter: MapGridPainter(),
          ),
          Positioned(
            bottom: 24,
            right: 20,
            child: FloatingActionButton(
              backgroundColor: VoltColors.green,
              foregroundColor: VoltColors.darkBg,
              onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const QRScannerScreen())),
              child: const Icon(Icons.qr_code_scanner, size: 28),
            ),
          ),
        ],
      ),
    );
  }
}

class MapGridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final linePaint = Paint()
      ..color = VoltColors.cardBorder.withOpacity(0.5)
      ..strokeWidth = 1.0;

    for (double i = 0; i < size.width; i += 40) {
      canvas.drawLine(Offset(i, 0), Offset(i, size.height), linePaint);
    }
    for (double i = 0; i < size.height; i += 40) {
      canvas.drawLine(Offset(0, i), Offset(size.width, i), linePaint);
    }

    _drawPin(canvas, Offset(size.width * 0.35, size.height * 0.40), VoltColors.green);
    _drawPin(canvas, Offset(size.width * 0.65, size.height * 0.55), VoltColors.cyan);
    _drawPin(canvas, Offset(size.width * 0.50, size.height * 0.70), Colors.orange);
  }

  void _drawPin(Canvas canvas, Offset pos, Color c) {
    final pinPaint = Paint()..color = c;
    canvas.drawCircle(pos, 10, pinPaint);
    canvas.drawCircle(pos, 16, pinPaint..color = c.withOpacity(0.3));
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}

// ============================================================================
// 15. TRIP ESTIMATOR SCREEN
// ============================================================================
class TripEstimatorScreen extends StatefulWidget {
  const TripEstimatorScreen({super.key});

  @override
  State<TripEstimatorScreen> createState() => _TripEstimatorScreenState();
}

class _TripEstimatorScreenState extends State<TripEstimatorScreen> {
  double _distanceKm = 250.0;

  @override
  Widget build(BuildContext context) {
    final volt = VoltScope.of(context);
    final rangeKm = volt.vehicle.realWorldRangeKm;
    final usableRange = rangeKm * (volt.vehicle.currentBatteryPercent / 100.0);
    final neededStops = (_distanceKm <= usableRange) ? 0 : (((_distanceKm - usableRange) / (rangeKm * 0.7)).ceil());

    return Scaffold(
      appBar: AppBar(title: const Text('Trip & Range Estimator'), backgroundColor: Colors.transparent),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Trip Distance: ${_distanceKm.toInt()} km', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            Slider(
              value: _distanceKm,
              min: 50,
              max: 600,
              activeColor: VoltColors.green,
              onChanged: (v) => setState(() => _distanceKm = v),
            ),
            const SizedBox(height: 20),
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(color: VoltColors.card, borderRadius: BorderRadius.circular(16), border: Border.all(color: VoltColors.cardBorder)),
              child: Column(
                children: [
                  _stat('Vehicle', '${volt.vehicle.make} ${volt.vehicle.model}'),
                  _stat('Usable Range', '${usableRange.toInt()} km (${volt.vehicle.currentBatteryPercent}%)'),
                  _stat('Recommended Charging Stops', '$neededStops ${neededStops == 1 ? "Stop" : "Stops"}'),
                  _stat('Estimated Charging Cost', '₹${neededStops * 340}'),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _stat(String l, String v) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
        Text(l, style: const TextStyle(color: VoltColors.textSecondary)),
        Text(v, style: const TextStyle(fontWeight: FontWeight.bold, color: VoltColors.cyan)),
      ]),
    );
  }
}
