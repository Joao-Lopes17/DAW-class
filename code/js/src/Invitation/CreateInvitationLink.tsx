import * as React from "react";
import { Navigate, useParams } from "react-router-dom";
import { AuthContext } from "../User/Auth/AuthProvider";

export function CreateInvitationLink() {
    const { username: inviterUsername } = React.useContext(AuthContext);
    const { channelId } = useParams();
    const [state, dispatch] = React.useReducer(reduce, {
        tag: "editing",
        inputs: {
            role: "READ_ONLY",
        },
    });

    React.useEffect(() => {
        if (state.tag === "submitting") {
            const { role } = state.inputs;
            createInvitation(inviterUsername, role, parseInt(channelId!))
                .then((res) => {
                    dispatch( {type: "success", code: res.code});
                })
                .catch((err) => dispatch({ type: "error", message: err.message }));
        }
    }, [state, inviterUsername, channelId]);

    if (state.tag === "success") {
        return (
            <div>
                <h1>Invitation Created Successfully!</h1>
                <p>Code: {state.code}</p>
            </div>
        );
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
            inputValue: event.target.value,
        });
    }

    const role = state.tag === "editing" ? state.inputs.role : "";

    return (
        <form onSubmit={handleSubmit}>
            <fieldset disabled={state.tag !== "editing"}>
               
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
                        inputs: { ...state.inputs, role: action.inputValue },
                        error: undefined,
                    };
                case "submit":
                    return { tag: "submitting", inputs: state.inputs };
            }
            break;
        case "submitting":
            switch (action.type) {
                case "success":
                    return { tag: "success", code: action.code };
                case "error":
                    return {
                        tag: "editing",
                        error: action.message,
                        inputs: { role: "READ_ONLY" }, // Default role
                    };
            }
            break;
        case "success":
            throw new Error(
                "Already in final State 'success' and should not reduce to any other State."
            );
    }
    return state;
}

type State =
    | { tag: "editing"; error?: string; inputs: { role: string } }
    | { tag: "submitting"; inputs: {  role: string } }
    | { tag: "success", code: string };

type Action =
    | { type: "edit"; inputValue: string }
    | { type: "submit" }
    | { type: "success"; code: string }
    | { type: "error"; message: string };

function createInvitation(
    inviterUsername: string,
    role: string,
    channelId: number
): Promise<{code: string}> {
    return fetch("http://localhost:8080/api/invitations", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
            inviterName: inviterUsername,
            channelId: channelId,
            type: role,
        }),
    })
        .then(async (response) => {
            if (!response.ok){
                const errorData = await response.json()
                throw new Error(errorData.message || "Failed to create invitation")
            }
        
            return response.json()
        })
        .catch((err) => {
            console.error(err);
            throw new Error("Failed to create invitation");
        });
}