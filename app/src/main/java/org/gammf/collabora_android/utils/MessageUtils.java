package org.gammf.collabora_android.utils;

import org.gammf.collabora_android.app.utils.ExceptionManager;
import org.gammf.collabora_android.collaborations.general.Collaboration;
import org.gammf.collabora_android.communication.allCollaborations.AllCollaborationsMessage;
import org.gammf.collabora_android.communication.allCollaborations.ConcreteAllCollaborationsMessage;
import org.gammf.collabora_android.communication.collaboration.CollaborationMessage;
import org.gammf.collabora_android.communication.collaboration.ConcreteCollaborationMessage;
import org.gammf.collabora_android.communication.common.Message;
import org.gammf.collabora_android.communication.error.ConcreteErrorMessage;
import org.gammf.collabora_android.communication.error.ErrorMessage;
import org.gammf.collabora_android.communication.update.collaborations.CollaborationUpdateMessage;
import org.gammf.collabora_android.communication.update.collaborations.ConcreteCollaborationUpdateMessage;
import org.gammf.collabora_android.communication.update.general.UpdateMessageTarget;
import org.gammf.collabora_android.communication.update.general.UpdateMessageType;
import org.gammf.collabora_android.communication.update.members.ConcreteMemberUpdateMessage;
import org.gammf.collabora_android.communication.update.members.MemberUpdateMessage;
import org.gammf.collabora_android.communication.update.modules.ConcreteModuleUpdateMessage;
import org.gammf.collabora_android.communication.update.modules.ModuleUpdateMessage;
import org.gammf.collabora_android.communication.update.notes.ConcreteNoteUpdateMessage;
import org.gammf.collabora_android.communication.update.notes.NoteUpdateMessage;
import org.gammf.collabora_android.communication.update.general.UpdateMessage;

import org.gammf.collabora_android.modules.Module;
import org.gammf.collabora_android.notes.Note;
import org.gammf.collabora_android.users.CollaborationMember;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Utily class providing methods to convert from UpdateMessage class to json message and vice versa.
 */

public class MessageUtils {

    /**
     * Provides a json with all the update message information.
     * @param message the update message.
     * @return a json message with all the update message information.
     */
    public static JSONObject updateMessageToJSON(final UpdateMessage message) {
        final JSONObject jsn = new JSONObject();
        try {
            jsn.put("user", message.getUsername())
                    .put("target", message.getTarget().name())
                    .put("messageType", message.getUpdateType().name())
                    .put("collaborationId", message.getCollaborationId());
            switch (message.getTarget()) {
                case NOTE:
                    jsn.put("note", NoteUtils.noteToJSON(((NoteUpdateMessage) message).getNote()));
                    break;
                case MODULE:
                    jsn.put("module", ModulesUtils.moduleToJson(((ModuleUpdateMessage) message).getModule()));
                    break;
                case COLLABORATION:
                    jsn.put("collaboration", CollaborationUtils.collaborationToJson(((CollaborationUpdateMessage) message).getCollaboration()));
                    break;
                case MEMBER:
                    jsn.put("member", CollaborationMemberUtils.memberToJson(((MemberUpdateMessage) message).getMember()));
                    break;
            }
        } catch (final JSONException e) {
            ExceptionManager.getInstance().handle(e);
        }
        return jsn;
    }

    /**
     * Creates an update message from a json message.
     * @param json the input json message.
     * @return an update message built from the json message.
     */
    public static UpdateMessage jsonToUpdateMessage(final JSONObject json) {
        UpdateMessage message = null;
        try {
            final String username = json.getString("user");
            final UpdateMessageType updateType = UpdateMessageType.valueOf(json.getString("messageType"));
            final String collaborationId = json.getString("collaborationId");

            final UpdateMessageTarget target = UpdateMessageTarget.valueOf(json.getString("target"));
            switch (target) {
                case NOTE:
                    final Note note = NoteUtils.jsonToNote(json.getJSONObject("note"));
                    message = new ConcreteNoteUpdateMessage(username, note, updateType, collaborationId);
                    break;
                case COLLABORATION:
                    final Collaboration collaboration = CollaborationUtils.jsonToCollaboration(json.getJSONObject("collaboration"));
                    message = new ConcreteCollaborationUpdateMessage(username, collaboration, updateType);
                    break;
                case MODULE:
                    final Module module = ModulesUtils.jsonToModule(json.getJSONObject("module"));
                    message = new ConcreteModuleUpdateMessage(username, module, updateType, collaborationId);
                    break;
                case MEMBER:
                    final CollaborationMember member = CollaborationMemberUtils.jsonToMember(json.getJSONObject("member"));
                    message = new ConcreteMemberUpdateMessage(username, member, updateType, collaborationId);
                    break;
                default:
                    throw new JSONException("JSON message not parsable! Target field is incorrect.");
            }
        } catch (final JSONException e) {
            ExceptionManager.getInstance().handle(e);
        }
        return message;
    }

    /**
     * Creates a message containing one or more collaborations from a json message.
     * @param json the input json message.
     * @return a collaborations message built from the json message.
     */
    public static Message jsonToCollaborationsMessage(final JSONObject json) {
        if (json.has("errorCode")) {
            return jsonToErrorMessage(json);
        } else if (json.has("collaboration")) {
            return jsonToCollaborationMessage(json);
        } else {
            return jsonToAllCollaborationsMessage(json);
        }
    }

    private static ErrorMessage jsonToErrorMessage(final JSONObject json) {
        ErrorMessage message = null;
        try {
            final String username = json.getString("user");
            final CollaborationError error = CollaborationError.valueOf(json.getString("errorCode"));
            message = new ConcreteErrorMessage(username, error);
        } catch (final JSONException e) {
            ExceptionManager.getInstance().handle(e);
        }
        return message;
    }

    private static CollaborationMessage jsonToCollaborationMessage(final JSONObject json) {
        CollaborationMessage message = null;
        try {
            final String username = json.getString("user");
            final Collaboration collaboration = CollaborationUtils.jsonToCollaboration(json.getJSONObject("collaboration"));
            message = new ConcreteCollaborationMessage(username, collaboration);
        } catch (final JSONException e) {
            ExceptionManager.getInstance().handle(e);
        }
        return message;
    }

    private static AllCollaborationsMessage jsonToAllCollaborationsMessage(final JSONObject json) {
        AllCollaborationsMessage message = null;
        try {
            final String username = json.getString("username");
            final List<Collaboration> collaborations = new ArrayList<>();
            final JSONArray jCollaborations = json.getJSONArray("collaborationList");
            for (int i = 0; i < jCollaborations.length(); i++) {
                collaborations.add(CollaborationUtils.jsonToCollaboration(jCollaborations.getJSONObject(i)));
            }
            message = new ConcreteAllCollaborationsMessage(username, collaborations);
        } catch (final JSONException e) {
            ExceptionManager.getInstance().handle(e);
        }
        return message;
    }
}
