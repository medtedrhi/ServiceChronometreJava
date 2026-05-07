package com.example.servicechronometrejava; // Déclare le paquet Java de cette activité.

import android.Manifest; // Importe la constante de permission pour les notifications.
import android.content.ComponentName; // Importe le nom du composant connecté au service.
import android.content.Context; // Importe le contexte Android utilisé pour binder le service.
import android.content.Intent; // Importe Intent pour démarrer, binder et arrêter le service.
import android.content.ServiceConnection; // Importe l'interface qui reçoit la connexion au service.
import android.os.Build; // Importe Build pour vérifier la version Android.
import android.os.Bundle; // Importe Bundle pour recevoir l'état de l'activité.
import android.os.Handler; // Importe Handler pour mettre à jour l'interface régulièrement.
import android.os.IBinder; // Importe IBinder pour récupérer le Binder du service.
import android.os.Looper; // Importe Looper pour exécuter le Handler sur le thread principal.
import android.widget.Button; // Importe Button pour les boutons de l'interface.
import android.widget.TextView; // Importe TextView pour afficher le chronomètre.

import androidx.appcompat.app.AppCompatActivity; // Importe AppCompatActivity comme demandé dans l'énoncé.

public class MainActivity extends AppCompatActivity { // Déclare l'activité principale de l'application.

    private TextView tvTemps; // Stocke le TextView qui affiche le temps.
    private ChronometreService chronometreService; // Stocke la référence au service une fois connecté.
    private boolean isBound = false; // Indique si l'activité est actuellement liée au service.
    private final Handler handler = new Handler(Looper.getMainLooper()); // Crée un Handler sur le thread principal.

    private final Runnable updateRunnable = new Runnable() { // Crée la tâche répétée qui rafraîchit l'heure.
        @Override // Indique que la méthode run vient de Runnable.
        public void run() { // Exécute le rafraîchissement du TextView.
            if (isBound && chronometreService != null) { // Vérifie que le service est connecté avant de lire le temps.
                tvTemps.setText(chronometreService.getFormattedTime()); // Affiche le temps courant du service.
            } // Termine la condition de service connecté.
            handler.postDelayed(this, 1000); // Relance cette tâche dans une seconde.
        } // Termine la méthode run.
    }; // Termine la déclaration du Runnable.

    private final ServiceConnection serviceConnection = new ServiceConnection() { // Crée l'objet qui gère la connexion au service.
        @Override // Indique que la méthode vient de ServiceConnection.
        public void onServiceConnected(ComponentName name, IBinder service) { // Réagit quand Android connecte l'activité au service.
            ChronometreService.LocalBinder binder = (ChronometreService.LocalBinder) service; // Convertit le Binder reçu en Binder local.
            chronometreService = binder.getService(); // Récupère l'instance du service.
            isBound = true; // Mémorise que l'activité est liée au service.
            tvTemps.setText(chronometreService.getFormattedTime()); // Affiche immédiatement le temps courant.
        } // Termine la méthode onServiceConnected.

        @Override // Indique que la méthode vient de ServiceConnection.
        public void onServiceDisconnected(ComponentName name) { // Réagit si Android coupe la connexion au service.
            isBound = false; // Mémorise que l'activité n'est plus liée.
            chronometreService = null; // Efface la référence vers le service.
        } // Termine la méthode onServiceDisconnected.
    }; // Termine la déclaration de ServiceConnection.

    @Override // Indique que la méthode vient de AppCompatActivity.
    protected void onCreate(Bundle savedInstanceState) { // Crée l'écran principal de l'application.
        super.onCreate(savedInstanceState); // Appelle le comportement normal de l'activité.
        setContentView(R.layout.activity_main); // Charge l'interface XML de l'activité.
        tvTemps = findViewById(R.id.tvTemps); // Récupère le TextView du chronomètre.
        Button btnStart = findViewById(R.id.btnStart); // Récupère le bouton de démarrage.
        Button btnStop = findViewById(R.id.btnStop); // Récupère le bouton d'arrêt.
        demanderPermissionNotification(); // Demande la permission de notification sur Android 13 ou plus.
        btnStart.setOnClickListener(v -> demarrerService()); // Démarre le service quand l'utilisateur appuie sur le bouton.
        btnStop.setOnClickListener(v -> arreterService()); // Arrête le service quand l'utilisateur appuie sur le bouton.
        connecterServiceSiActif(); // Reconnecte l'activité si le service tourne déjà en arrière-plan.
        handler.post(updateRunnable); // Lance les mises à jour visuelles du chronomètre.
    } // Termine la méthode onCreate.

    private void demanderPermissionNotification() { // Déclare la méthode qui demande la permission de notification.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Vérifie si l'appareil utilise Android 13 ou plus.
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100); // Demande la permission POST_NOTIFICATIONS.
        } // Termine la condition de version Android.
    } // Termine la méthode de demande de permission.

    private void demarrerService() { // Déclare la méthode qui démarre et connecte le service.
        Intent intent = new Intent(this, ChronometreService.class); // Crée un Intent vers ChronometreService.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Vérifie si Android demande startForegroundService.
            startForegroundService(intent); // Démarre le service au premier plan sur Android 8 ou plus.
        } else { // Gère les versions Android plus anciennes.
            startService(intent); // Démarre le service normalement avant Android 8.
        } // Termine le choix de méthode de démarrage.
        if (!isBound) { // Vérifie que l'activité n'est pas déjà liée au service.
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE); // Lie l'activité au service pour lire le temps.
        } // Termine la condition de connexion.
    } // Termine la méthode de démarrage.

    private void connecterServiceSiActif() { // Déclare la méthode qui se reconnecte à un service déjà lancé.
        if (ChronometreService.isServiceActif()) { // Vérifie si le service indique qu'il tourne déjà.
            Intent intent = new Intent(this, ChronometreService.class); // Crée un Intent vers le service existant.
            bindService(intent, serviceConnection, 0); // Se lie au service sans le créer s'il n'existe pas.
        } // Termine la condition de reconnexion.
    } // Termine la méthode de reconnexion.

    private void arreterService() { // Déclare la méthode qui arrête le service.
        if (isBound) { // Vérifie si l'activité est liée au service.
            unbindService(serviceConnection); // Détache l'activité du service.
            isBound = false; // Mémorise que le service n'est plus lié.
            chronometreService = null; // Efface la référence vers le service.
        } // Termine la condition de détachement.
        Intent stopIntent = new Intent(this, ChronometreService.class); // Crée un Intent vers le service.
        stopIntent.setAction(ChronometreService.ACTION_STOP); // Ajoute l'action STOP demandée.
        startService(stopIntent); // Envoie l'ordre d'arrêt au service.
        tvTemps.setText("00:00"); // Remet le TextView à zéro.
    } // Termine la méthode d'arrêt.

    @Override // Indique que la méthode vient de AppCompatActivity.
    protected void onDestroy() { // Réagit quand l'activité est détruite.
        handler.removeCallbacks(updateRunnable); // Arrête les mises à jour de l'interface.
        if (isBound) { // Vérifie si l'activité est encore liée au service.
            unbindService(serviceConnection); // Détache l'activité du service proprement.
            isBound = false; // Mémorise que le service n'est plus lié.
        } // Termine la condition de détachement.
        super.onDestroy(); // Appelle le comportement normal de destruction.
    } // Termine la méthode onDestroy.
} // Termine la classe MainActivity.
