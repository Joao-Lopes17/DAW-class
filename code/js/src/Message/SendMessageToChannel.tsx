import { useParams } from "react-router";
import * as React from "react";
import { AuthContext } from "../User/Auth/AuthProvider";

export function SendMessageToChannel() {
    const { username } = React.useContext(AuthContext);
    const { channelId } = useParams();
    const [state, dispatch] = React.useReducer(reduce, {
        tag: "editing",
        inputs: {
            content: "",
        },
    });

    React.useEffect(() => {
        if (state.tag === "submitting") {
            const { content } = state.inputs;
            SendMessageToChannelInBackend(content, username, channelId!)
                .then((res) => {
                    dispatch(
                        res
                            ? { type: "success" }
                            : { type: "error", message: "You don't have permission" }
                    );
                })
                .catch((err) => {
                    dispatch({ type: "error", message: err.message });
                });
        }
    }, [state]);

    function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (state.tag !== "editing") return;
        dispatch({ type: "submit" });
    }

    function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
        dispatch({
            type: "edit",
            inputName: event.target.name,
            inputValue: event.target.value,
        });
    }

    const msg = state.tag === "editing" ? state.inputs.content : "";

    return (
        <form onSubmit={handleSubmit}>
            <fieldset disabled={state.tag !== "editing"}>
                <div>
                    <label htmlFor="message">Message</label>
                    <input
                        id="content"
                        type="text"
                        name="content"
                        value={msg}
                        onChange={handleChange}
                    />
                </div>
                <div>
                    <button type="submit" disabled={state.tag === "submitting"}>
                        {state.tag === "submitting" ? "Sending" : "Send message"}
                    </button>
                </div>
            </fieldset>
            {state.tag === "editing" && state.error && <p>{state.error}</p>}
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
        case "submitting":
            switch (action.type) {
                case "success":
                    return { tag: "redirect" };
                case "error":
                    return {
                        tag: "editing",
                        error: action.message,
                        inputs: { content: "" },
                    };
            }
        case "redirect":
            throw Error("Already in final State 'redirect' and should not reduce to any other State.");
    }
}

type State =
    | { tag: "editing"; error?: string; inputs: { content: string } }
    | { tag: "submitting"; inputs: { content: string } }
    | { tag: "redirect" };

type Action =
    | { type: "edit"; inputName: string; inputValue: string }
    | { type: "submit" }
    | { type: "success" }
    | { type: "error"; message: string };

function SendMessageToChannelInBackend(
    content: string,
    username: string,
    channelId: string
): Promise<boolean> {
    return fetch(`http://localhost:8080/api/channel/${channelId}/messages`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify({
            username: username,
            channelId: channelId,
            content: content,
        }),
    })
        .then((response) => {
            if (response.status === 403) {
                return false;
            }
            return true;
        })
        .catch((err) => {
            console.error(err);
            throw new Error("Failed to send the message");
        });
}