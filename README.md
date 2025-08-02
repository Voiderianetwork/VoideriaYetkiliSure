VoideriaYetkiliSüre Plugin - Minecraft Yetkili Takip Sistemi
Plugin Adı: VoideriaYetkiliSüre
Versiyon: 1.0.0
Desteklenen Platformlar: Paper/Spigot 1.21+ (Folia Desteği Dahil!)
Geliştirici: Voideria Türkiye
İndirme: GitHub Repo

🔍 Plugin Nedir?
VoideriaYetkiliSüre, Minecraft sunucularında yetkili sistemini tamamen otomatikleştiren devrim niteliğinde bir eklentidir. Yetkililerin görev sürelerini takip eder, başarılarına göre otomatik ödüllendirir ve haftalık performans raporları oluşturur.

✨ Temel Özellikler
Otomatik Süre Takibi

Yetkililerin sunucuda geçirdiği süreyi saniye bazında kaydeder

Çevrimdışı oyuncuların süreleri bile takip edilir

Akıllı Rütbe Sistemi

Özelleştirilebilir rütbe kademeleri

Her rütbe için farklı süre gereksinimleri

ranks:
  rehber:
    required-hours: 10 # 10 saat
    reward-command: "eco give {player} 5000"
Ödül Mekanizması

Süre hedefleri tamamlandığında otomatik ödül verme

Özel ödül mesajları ve komut entegrasyonu

reward-message: "&aTebrikler! &6{rank} &arütbesi için 5.000 TL kazandınız!"
Haftalık Otomatik Sıfırlama

Perşembe 19:00'da tüm süreler sıfırlanır

Özelleştirilebilir sıfırlama zamanı

reset:
  day-of-week: "THURSDAY"
  time: "19:00"
Discord Entegrasyonu

Haftalık performans raporları

Top 10 yetkili listesi

discord:
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/..."
🛠️ Komut Sistemi
Komut	Açıklama	Örnek
/yetkilisure ver	Oyuncuya rütbe verir	/yetkilisure ver Player123 rehber
/yetkilisure liste	Tüm yetkili sürelerini listeler	/yetkilisure liste
/yetkilisure ekle	Manuel süre ekler	/yetkilisure ekle Player123 120
/yetkilisure rapor	Discord'a anlık rapor gönderir	/yetkilisure rapor
/yetkilisure sifirla	Tüm süreleri sıfırlar	/yetkilisure sifirla
/yetkilisure yenile	Plugin'i yeniden yükler	/yetkilisure yenile

⚙️ Teknik Özellikler
Folia Desteği: 1.21+ sunucularda kusursuz çalışır

Veri Güvenliği: Oyuncu verileri düzenli yedeklenir

Performans Dostu: 100+ oyunculu sunucularda bile sıfır lag
📥 Kurulum
VoideriaYetkiliSure.jar dosyasını plugins klasörüne atın

Sunucuyu yeniden başlatın

plugins/VoideriaYetkiliSure/config.yml dosyasını düzenleyin

/yetkilisure yenile komutuyla ayarları aktif edin

❓ Neden VoideriaYetkiliSüre?
✅ %100 Türk yapımı ve yerel desteği

✅ 7/24 aktif geliştirici ekibi

✅ Düzenli güncellemeler ve yeni özellikler

✅ Ücretsiz ve açık kaynak kodlu

📞 Destek & İletişim
Discord: https://discord.gg/s3QXrmhrEg

GitHub: [Hata Bildirimi](https://github.com/Voiderianetwork/VoideriaYetkiliSure/issues)

+ 50'den Fazla Sunucu Tarafından Güvenilir!
- Artık Yetkili Takibi İçin Saatler Harcamanıza Gerek Yok!
Ücretsiz İndir, Hemen Kur, Zamandan Kazan! 🚀
