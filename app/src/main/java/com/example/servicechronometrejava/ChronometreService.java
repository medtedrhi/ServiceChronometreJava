package com.example.servicechronometrejava; // Déclare le paquet Java du service.

import android.app.Notification; // Importe la classe Notification affichée au premier plan.
import android.app.NotificationChannel; // Importe NotificationChannel pour Android 8 ou plus.
import android.app.NotificationManager; // Importe NotificationManager pour créer et modifier la notification.
import android.app.PendingIntent; // Importe PendingIntent pour ouvrir l'activité depuis la notification.
import android.app.Service; // Importe Service pour créer le service Android.
import android.content.Intent; // Importe Intent pour recevoir les actions envoyées au service.
import android.os.Binder; // Importe Binder pour le service lié.
import android.os.Build; // Importe Build pour vérifier la version Android.
import android.os.IBinder; // Importe IBinder comme type de retour de onBind.

import androidx.core.app.NotificationCompat; // Importe NotificationCompat pour une notification compatible.

import java.util.Locale; // Importe Locale pour formater le temps de manière stable.
import java.util.concurrent.ScheduledExecutorService; // Importe ScheduledExecutorService pour lancer le timer.
import java.util.concurrent.Executors; // Importe Executors pour créer l'exécuteur du timer.
import java.util.concurrent.TimeUnit; // Importe TimeUnit pour exprimer un délai en secondes.
import java.util.concurrent.atomic.AtomicInteger; // Importe AtomicInteger pour compter les secondes sans conflit de thread.

public class ChronometreService extends Service { // Déclare le service de chronomètre.

    public static final String ACTION_STOP = "STOP"; // Déclare l'action utilisée pour arrêter le service.
    private static final String CHANNEL_ID = "chronometre_channel"; // Déclare l'identifiant du canal de notification.
    private static final int NOTIFICATION_ID = 1; // Déclare l'identifiant unique de la notification.
    private static volatile boolean serviceActif = false; // Indique aux activités si le service tourne déjà.

    private final IBinder binder = new LocalBinder(); // Crée le Binder local donné aux activités liées.
    private final AtomicInteger secondes = new AtomicInteger(0); // Stocke le nombre de secondes écoulées.
    private ScheduledExecutorService executorService; // Stocke l'exécuteur qui fait avancer le chronomètre.
    private NotificationManager notificationManager; // Stocke le gestionnaire de notifications.
    private boolean isRunning = false; // Indique si le chronomètre est en cours.

    public class LocalBinder extends Binder { // Déclare le Binder local du Bound Service.
        public ChronometreService getService() { // Donne accès à l'instance du service.
            return ChronometreService.this; // Retourne ce service.
        } // Termine la méthode getService.
    } // Termine la classe LocalBinder.

    @Override // Indique que la méthode vient de Service.
    public void onCreate() { // Initialise le service quand Android le crée.
        super.onCreate(); // Appelle le comportement normal du service.
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE); // Récupère le gestionnaire de notifications.
        creerCanalNotification(); // Crée le canal de notification si nécessaire.
    } // Termine la méthode onCreate.

    @Override // Indique que la méthode vient de Service.
    public int onStartCommand(Intent intent, int flags, int startId) { // Reçoit les commandes envoyées au service.
        if (intent != null && ACTION_STOP.equals(intent.getAction())) { // Vérifie si l'action reçue demande l'arrêt.
            arreterChronometre(); // Arrête le timer et la notification.
            stopSelf(); // Demande à Android d'arrêter ce service.
            return START_NOT_STICKY; // Indique que le service ne doit pas redémarrer après STOP.
        } // Termine la condition d'arrêt.
        startForeground(NOTIFICATION_ID, creerNotification()); // Place le service au premier plan avec une notification.
        demarrerChronometre(); // Lance le timer si ce n'est pas déjà fait.
        return START_STICKY; // Demande à Android de recréer le service si nécessaire.
    } // Termine la méthode onStartCommand.

    @Override // Indique que la méthode vient de Service.
    public IBinder onBind(Intent intent) { // Donne un Binder aux activités qui se connectent.
        return binder; // Retourne le Binder local.
    } // Termine la méthode onBind.

    private void demarrerChronometre() { // Déclare la méthode qui démarre le timer.
        if (isRunning) { // Vérifie si le timer tourne déjà.
            return; // Évite de créer plusieurs timers.
        } // Termine la condition de protection.
        isRunning = true; // Mémorise que le timer est actif.
        serviceActif = true; // Mémorise globalement que le service tourne.
        executorService = Executors.newSingleThreadScheduledExecutor(); // Crée un exécuteur avec un seul thread.
        executorService.scheduleAtFixedRate(() -> { // Planifie une action répétée chaque seconde.
            secondes.incrementAndGet(); // Ajoute une seconde au chronomètre.
            mettreAJourNotification(); // Rafraîchit le texte de la notification.
        }, 1, 1, TimeUnit.SECONDS); // Configure le premier lancement après une seconde puis toutes les secondes.
    } // Termine la méthode de démarrage du timer.

    private void arreterChronometre() { // Déclare la méthode qui arrête le timer.
        isRunning = false; // Mémorise que le timer n'est plus actif.
        serviceActif = false; // Mémorise globalement que le service ne tourne plus.
        secondes.set(0); // Remet le compteur à zéro.
        if (executorService != null) { // Vérifie qu'un exécuteur existe.
            executorService.shutdownNow(); // Arrête immédiatement le timer.
            executorService = null; // Efface la référence vers l'exécuteur.
        } // Termine la condition d'arrêt de l'exécuteur.
        stopForeground(true); // Retire la notification persistante.
    } // Termine la méthode d'arrêt du timer.

    private void creerCanalNotification() { // Déclare la méthode qui crée le canal de notification.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Vérifie si Android utilise les canaux de notification.
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Chronomètre", NotificationManager.IMPORTANCE_LOW); // Crée un canal discret pour le service.
            channel.setDescription("Notification persistante du chronomètre"); // Ajoute une description au canal.
            notificationManager.createNotificationChannel(channel); // Enregistre le canal auprès d'Android.
        } // Termine la condition de version Android.
    } // Termine la méthode de création du canal.

    private Notification creerNotification() { // Déclare la méthode qui construit la notification.
        Intent activityIntent = new Intent(this, MainActivity.class); // Crée un Intent pour rouvrir l'activité.
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE); // Crée l'action cliquable de la notification.
        return new NotificationCompat.Builder(this, CHANNEL_ID) // Commence la construction de la notification compatible.
                .setSmallIcon(R.mipmap.ic_launcher) // Définit la petite icône de notification.
                .setContentTitle("Service Chronomètre") // Définit le titre de la notification.
                .setContentText("Temps : " + getFormattedTime()) // Définit le texte avec le temps courant.
                .setContentIntent(pendingIntent) // Ouvre l'activité quand l'utilisateur touche la notification.
                .setOngoing(true) // Rend la notification persistante.
                .setOnlyAlertOnce(true) // Évite de faire sonner la notification à chaque mise à jour.
                .build(); // Construit l'objet Notification final.
    } // Termine la méthode de création de notification.

    private void mettreAJourNotification() { // Déclare la méthode qui rafraîchit la notification.
        if (notificationManager != null && isRunning) { // Vérifie que le gestionnaire existe et que le timer tourne.
            notificationManager.notify(NOTIFICATION_ID, creerNotification()); // Remplace la notification avec le nouveau temps.
        } // Termine la condition de mise à jour.
    } // Termine la méthode de mise à jour.

    public String getFormattedTime() { // Déclare la méthode lue par MainActivity.
        return formatTemps(secondes.get()); // Retourne le temps courant en MM:SS.
    } // Termine la méthode getFormattedTime.

    public static boolean isServiceActif() { // Déclare la méthode qui indique si le service est déjà actif.
        return serviceActif; // Retourne l'état partagé du service.
    } // Termine la méthode isServiceActif.

    public String formatTemps(int sec) { // Déclare la méthode qui convertit les secondes en texte.
        int minutes = sec / 60; // Calcule le nombre de minutes.
        int secondesRestantes = sec % 60; // Calcule les secondes restantes.
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secondesRestantes); // Retourne le texte au format MM:SS.
    } // Termine la méthode formatTemps.

    @Override // Indique que la méthode vient de Service.
    public void onDestroy() { // Nettoie le service avant sa destruction.
        arreterChronometre(); // Arrête le timer et retire la notification.
        super.onDestroy(); // Appelle le comportement normal de destruction.
    } // Termine la méthode onDestroy.
} // Termine la classe ChronometreService.
