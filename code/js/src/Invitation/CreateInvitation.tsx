import * as React from "react";
import { Navigate, useParams } from "react-router-dom";
import { AuthContext } from "../User/Auth/AuthProvider";

export function CreateInvitation() {
    const { username: inviterUsername } = React.useContext(AuthContext);
    const { channelId } = useParams();
    const [state, dispatch] = React.useReducer(reduce, {
        tag: "editing",
        inputs: {
            inviteeUsername: "",
            role: "READ_ONLY",
        },
    });

    React.useEffect(() => {
        if (state.tag === "submitting") {
            const { inviteeUsername, role } = state.inputs;
            createInvitation(inviterUsername, inviteeUsername, role, parseInt(channelId!))
                .then((res) => {
                    dispatch(
                        res
                            ? { type: "success" }
                            : {
                                  type: "error",
                                  message: `Failed to create invitation for: ${inviteeUsername}`,
                              }
                    );
                })
                .catch((err) => dispatch({ type: "error", message: err.message }));
        }
    }, [state, inviterUsername, channelId]);

    if (state.tag === "redirect") {
        return <Navigate to={`/channels/${channelId}`} replace={true} />;
    }

    function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (state.tag !== "editing") {
            return;
        }
        dispatch({ type: "submit" });
    }

    function handleChange(event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
        dispatch({
            type: "edit",
            inputName: event.target.name,
            inputValue: event.target.value,
        });
    }

    const inviteeUsername = state.tag === "editing" ? state.inputs.inviteeUsername : "";
    const role = state.tag === "editing" ? state.inputs.role : "";

    return (
        <form onSubmit={handleSubmit}>
            <fieldset disabled={state.tag !== "editing"}>
                <div>
                    <label htmlFor="inviteeUsername">Invitee Username</label>
                    <input
                        id="inviteeUsername"
                        type="text"
                        name="inviteeUsername"
                        value={inviteeUsername}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="role">Role</label>
                    <select id="role" name="role" value={role} onChange={handleChange}>
                        <option value="READ_ONLY">Read-Only</option>
                        <option value="READ_WRITE">Read-Write</option>
                    </select>
                </div>
                <div>
                    <button type="submit" disabled={state.tag === "submitting"}>
                        {state.tag === "submitting" ? "Creating..." : "Create Invitation"}
                    </button>
                </div>
            </fieldset>
            {state.tag === "editing" && state.error && <p style={{ color: "red" }}>{state.error}</p>}
        </form>
    );
}

function reduce(state: State, action: Action): State {
    switch (state.tag) {
        case "editing":
            switch (action.type) {
                case "edit":
                    return {
                        tag: "editing",
                        inputs: { ...state.inputs, [action.inputName]: action.inputValue },
                        error: undefined,
                    };
                case "submit":
                    return { tag: "submitting", inputs: state.inputs };
            }
            break;
        case "submitting":
            switch (action.type) {
                case "success":
                    return { tag: "redirect" };
                case "error":
                    return {
                        tag: "editing",
                        error: action.message,
                        inputs: { inviteeUsername: "", role: "READ_ONLY" }, // Default role
                    };
            }
            break;
        case "redirect":
            throw new Error(
                "Already in final State 'redirect' and should not reduce to any other State."
            );
    }
    return state;
}

type State =
    | { tag: "editing"; error?: string; inputs: { inviteeUsername: string; role: string } }
    | { tag: "submitting"; inputs: { inviteeUsername: string; role: string } }
    | { tag: "redirect" };

type Action =
    | { type: "edit"; inputName: string; inputValue: string }
    | { type: "submit" }
    | { type: "success" }
    | { type: "error"; message: string };

function createInvitation(
    inviterUsername: string,
    inviteeUsername: string,
    role: string,
    channelId: number
): Promise<boolean> {
    return fetch("http://localhost:8080/api/invitations", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
            inviterName: inviterUsername,
            inviteeName: inviteeUsername,
            channelId: channelId,
            type: role,
        }),
    })
        .then((response) => response.ok)
        .catch((err) => {
            console.error(err);
            throw new Error("Failed to create invitation");
        });
}