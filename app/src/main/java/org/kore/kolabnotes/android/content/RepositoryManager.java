package org.kore.kolabnotes.android.content;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.kore.kolab.notes.Identification;
import org.kore.kolab.notes.Note;
import org.kore.kolab.notes.Notebook;
import org.kore.kolab.notes.NotesRepository;
import org.kore.kolab.notes.Tag;
import org.kore.kolab.notes.imap.ImapNotesRepository;
import org.kore.kolab.notes.imap.RemoteTags;
import org.kore.kolabnotes.android.DetailActivity;
import org.kore.kolabnotes.android.R;
import org.kore.kolabnotes.android.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

/**
 * This class syncs the database with data from a given repository
 *
 * Created by koni on 18.04.15.
 */
public class RepositoryManager {

    private final NoteTagRepository noteTagRepository;
    private final TagRepository tagRepository;
    private final NoteRepository noteRepository;
    private final NotebookRepository notebookRepository;
    private final ModificationRepository modificationRepository;
    private final Date lastSync;
    private final Context context;
    private final Set<String> localChangedNotes;

    private final ImapNotesRepository repo;

    public RepositoryManager(Context context, ImapNotesRepository repo, Date lastSync) {
        this.noteTagRepository = new NoteTagRepository(context);
        this.tagRepository = new TagRepository(context);
        this.noteRepository = new NoteRepository(context);
        this.notebookRepository = new NotebookRepository(context);
        this.modificationRepository = new ModificationRepository(context);
        this.repo = repo;
        this.lastSync = new Date(lastSync.getTime());
        this.context = context;
        this.localChangedNotes = new HashSet<>();
    }

    public void sync(String email, String rootFolder){
        putLocalDataIntoRepository(email, rootFolder);
        cleanLocalData(email,rootFolder);
        putDataIntoDB(email,rootFolder);
        modificationRepository.cleanAccount(email,rootFolder);
    }

    void putDataIntoDB(String email, String rootFolder){
        Collection<Notebook> notebooks = repo.getNotebooks();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        int i = 5;
        for(Notebook book : notebooks){
            notebookRepository.insert(email,rootFolder,book);

            for(Note note : book.getNotes()){
                noteRepository.insert(email,rootFolder,note,book.getIdentification().getUid());

                //inform user for new or updated notes in shared notebooks
                if(Utils.getShowSyncNotifications(context) && book.isShared() && !localChangedNotes.contains(note.getIdentification().getUid()) && lastSync != null){

                    Intent startDetailIntent = new Intent(context,DetailActivity.class);
                    startDetailIntent.setAction(UUID.randomUUID().toString());
                    startDetailIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startDetailIntent.putExtra(Utils.NOTE_UID, note.getIdentification().getUid());
                    startDetailIntent.putExtra(Utils.INTENT_ACCOUNT_EMAIL, email);
                    startDetailIntent.putExtra(Utils.INTENT_ACCOUNT_ROOT_FOLDER, rootFolder);

                    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, startDetailIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    if(lastSync.getTime() < note.getAuditInformation().getCreationDate().getTime()){
                        final Notification notification = new NotificationCompat.Builder(context)
                                .setSmallIcon(R.drawable.ic_kjots)
                                .setContentTitle(context.getResources().getString(R.string.changed_content_shared_folder))
                                .setContentText(context.getResources().getString(R.string.note_created))
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getResources().getString(R.string.note_created) + ": " + note.getSummary() + " " + context.getResources().getString(R.string.in_notebook) + " " + book.getSummary()))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true).build();

                        notificationManager.notify(i++,notification);
                    }else if(lastSync.getTime() < note.getAuditInformation().getLastModificationDate().getTime()){
                        final Notification notification = new NotificationCompat.Builder(context)
                                .setSmallIcon(R.drawable.ic_kjots)
                                .setContentTitle(context.getResources().getString(R.string.changed_content_shared_folder))
                                .setContentText(context.getResources().getString(R.string.note_changed))
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getResources().getString(R.string.note_changed) + ": " + note.getSummary() + " " + context.getResources().getString(R.string.in_notebook) + " " + book.getSummary()))
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true).build();

                        notificationManager.notify(i++,notification);
                    }
                }
            }
        }

        RemoteTags remoteTags = repo.getRemoteTags();

        for(RemoteTags.TagDetails detail : remoteTags.getTags()){
            final Tag tag = detail.getTag();
            String remoteName = tag.getName();
            tagRepository.insert(email,rootFolder, tag);

            for(String noteUid : detail.getMembers()){
                noteTagRepository.insert(email,rootFolder,noteUid,remoteName);
            }
        }
    }

    void cleanLocalData(String email, String rootFolder){
        noteRepository.cleanAccount(email,rootFolder);
        noteTagRepository.cleanAccount(email,rootFolder);
        tagRepository.cleanAccount(email,rootFolder);
    }

    private Notebook searchNotebookOfNote(NotesRepository repo, String noteUID){
        for(Notebook notebook : repo.getNotebooks()){
            if(notebook.getNote(noteUID) != null){
                return notebook;
            }
        }
        return null;
    }

    void putLocalDataIntoRepository(String email, String rootFolder){
        boolean withLatest = Utils.clearConflictWithLatest(context);
        boolean withLocal = Utils.clearConflictWithLocal(context);

        List<Note> localNotes = noteRepository.getAllForSync(email, rootFolder);

        List<Modification> deletedNbs = modificationRepository.getDeletions(email, rootFolder, Modification.Descriminator.NOTEBOOK);

        final RemoteTags remoteTags = repo.getRemoteTags();

        for(Note note : localNotes){
            Modification modification = modificationRepository.getUnique(email, rootFolder, note.getIdentification().getUid());

            if(modification != null){
                Notebook localNotebook = notebookRepository.getByUID(email, rootFolder, noteRepository.getUIDofNotebook(email, rootFolder, note.getIdentification().getUid()));
                Notebook remoteNotebook = repo.getNotebookBySummary(localNotebook.getSummary());

                if(remoteNotebook == null){
                    Log.d("localIntoRepository","Creating new notebook on server:"+localNotebook.getSummary());
                    remoteNotebook = repo.createNotebook(localNotebook.getIdentification().getUid(), localNotebook.getSummary());
                }

                if(ModificationRepository.ModificationType.INS.equals(modification.getType())){
                    Log.d("localIntoRepository","Creating new note:"+note);
                    remoteNotebook.addNote(note);

                    Set<Tag> localCategories = note.getCategories();
                    final Tag[] tagArray = localCategories.toArray(new Tag[localCategories.size()]);
                    remoteTags.attachTags(note.getIdentification().getUid(), tagArray);
                    localChangedNotes.add(note.getIdentification().getUid());
                }else{
                    Note remoteNote = remoteNotebook.getNote(note.getIdentification().getUid());

                    //if the notebook of the note was changed locally
                    if(remoteNote == null){
                        final Notebook notebook = searchNotebookOfNote(repo, note.getIdentification().getUid());

                        //the note exists in another notebook
                        if(notebook != null){
                            notebook.deleteNote(note.getIdentification().getUid());
                            remoteNotebook.addNote(note);
                            remoteNote = note;
                        }else if(withLocal){
                            //if local changes should always overrule server, recreate the note
                            remoteNotebook.addNote(note);
                            remoteNote = note;
                        }
                    }

                    if(remoteNote != null) {
                        //If there is a conflict
                        if (remoteNote.getAuditInformation().getLastModificationDate().after(lastSync)) {
                            if (withLatest) {
                                //if local note is newer then remote, update it, if not the remote will be taken
                                if (note.getAuditInformation().getLastModificationDate().after(remoteNote.getAuditInformation().getLastModificationDate())) {
                                    updateRemoteNote(remoteTags, note, remoteNote);
                                }
                            } else if (withLocal) {
                                updateRemoteNote(remoteTags, note, remoteNote);
                            }
                        } else {
                            updateRemoteNote(remoteTags, note, remoteNote);
                        }
                    }
                }
            }else{
                //Fill the unchanged, unloaded notes, so that in the later step, everything can be replaced in the local repo with data from the remote repo
                repo.fillUnloadedNote(note);
            }
        }

        List<Modification> deletions = modificationRepository.getDeletions(email, rootFolder, Modification.Descriminator.NOTE);
        for(Modification deletion : deletions){
            Note remoteNote = repo.getNote(deletion.getUid());

            if(remoteNote != null){
                if(withLatest){
                    if(deletion.getModificationDate().after(remoteNote.getAuditInformation().getLastModificationDate())){
                        Log.d("localIntoRepository","Deleting note:"+remoteNote);
                        Notebook localNotebook = notebookRepository.getByUID(email, rootFolder, deletion.getUidNotebook());
                        repo.getNotebookBySummary(localNotebook.getSummary()).deleteNote(deletion.getUid());
                    }
                }else if(withLocal){
                    Log.d("localIntoRepository","Deleting note:"+remoteNote);
                    Notebook localNotebook = notebookRepository.getByUID(email, rootFolder, deletion.getUidNotebook());
                    repo.getNotebookBySummary(localNotebook.getSummary()).deleteNote(deletion.getUid());
                }
            }
        }

        for(Modification deletion : deletedNbs){

            //Because Notebooks are IMAP-Folders, there is no persistent UID, so in the UidNotebook field, ist the summary of the notebook, which identifies the notebook persistent
            Notebook toDelete = repo.getNotebookBySummary(deletion.getUidNotebook());

            //Also there is no modification date, so if there is a notebook (folder) with this name, delete it
            if(toDelete != null){
                Log.d("localIntoRepository","Deleting notebook:"+toDelete.getSummary());
                repo.deleteNotebook(toDelete.getIdentification().getUid());
            }
        }

        //Update the tags
        final List<Tag> allModifiedAfter = tagRepository.getAllModifiedAfter(email, rootFolder, lastSync);
        remoteTags.applyLocalChanges(allModifiedAfter.toArray(new Tag[allModifiedAfter.size()]));

        List<Modification> deletedTags = modificationRepository.getDeletions(email, rootFolder, Modification.Descriminator.TAG);
        List<Identification> toDeleteUIDs = new ArrayList<>();
        for(Modification deletion : deletedTags){
            //in case of tags, is in the uidNotebook the tagname
            RemoteTags.TagDetails remoteTag = remoteTags.getTag(deletion.getUidNotebook());

            if(remoteTag != null){
                if(withLatest){
                    if(deletion.getModificationDate().after(remoteTag.getAuditInformation().getLastModificationDate())){
                        Log.d("localIntoRepository", "Deleting tag:" + remoteTag);
                    }
                    toDeleteUIDs.add(remoteTag.getIdentification());
                }else if(withLocal){
                    Log.d("localIntoRepository", "Deleting tag:" + remoteTag);
                    toDeleteUIDs.add(remoteTag.getIdentification());
                }
            }
        }
        remoteTags.deleteTags(toDeleteUIDs.toArray(new Identification[toDeleteUIDs.size()]));
    }

    private void updateRemoteNote(RemoteTags remoteTags, Note note, Note remoteNote) {
        Log.d("localIntoRepository", "Updating remote note:" + note);

        remoteNote.setClassification(note.getClassification());
        remoteNote.setDescription(note.getDescription());
        remoteNote.setSummary(note.getSummary());

        Set<Tag> remoteCategories = remoteNote.getCategories();

        remoteNote.removeCategories(remoteCategories.toArray(new Tag[remoteCategories.size()]));

        Set<Tag> localCategories = note.getCategories();
        final Tag[] tagArray = localCategories.toArray(new Tag[localCategories.size()]);
        remoteNote.addCategories(tagArray);
        remoteNote.setColor(note.getColor());
        remoteNote.getAuditInformation().setLastModificationDate(note.getAuditInformation().getLastModificationDate().getTime());
        remoteNote.getAuditInformation().setCreationDate(note.getAuditInformation().getCreationDate().getTime());

        remoteTags.removeTags(note.getIdentification().getUid());
        remoteTags.attachTags(note.getIdentification().getUid(), tagArray);

        localChangedNotes.add(note.getIdentification().getUid());
    }
}
