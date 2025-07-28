import * as React from "react";
import { Navigate } from "react-router-dom";
import { AuthContext } from "../User/Auth/AuthProvider";

export function CreateChannel() {
    const { username } = React.useContext(AuthContext);
    const [state, dispatch] = React.useReducer(reduce, {
        tag: "editing",
        inputs: {
            name: "",
            type: "",
        },
    });

    // UseEffect para lidar com o estado de submissÃ£o
    React.useEffect(() => {
        if (state.tag === "submitting") {
            const { name, type } = state.inputs;
            createChannel({ name, type, ownerName: username })
                .then((res) => {
                    dispatch(
                        res
                            ? { type: "success" }
                            : {
                                  type: "error",
                                  message: `Failed to create channel: ${name}`,
                              }
                    );
                })
                .catch((err) => dispatch({ type: "error", message: err.message }));
        }
    }, [state, username]);

    if (state.tag === "redirect") {
        return <Navigate to="/channels" replace={true} />;
    }

    function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (state.tag !== "editing") return;

        dispatch({ type: "submit" });
    }

    function handleChange(event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) {
        dispatch({ type: "edit", inputName: event.target.name, inputValue: event.target.value });
    }

    const { name, type } = state.tag === "editing" ? state.inputs : { name: "", type: "" };

    return (
        <form onSubmit={handleSubmit}>
            <fieldset disabled={state.tag !== "editing"}>
                <div>
                    <label htmlFor="channelName">Channel Name</label>
                    <input
                        id="channelName"
                        type="text"
                        name="name"
                        value={name}
                        onChange={handleChange}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="channelType">Channel Type</label>
                    <select id="channelType" name="type" value={type} onChange={handleChange} required>
                        <option value="">Select a type</option>
                        <option value="PUBLIC">Public</option>
                        <option value="PRIVATE">Private</option>
                    </select>
                </div>
                <div>
                    <button type="submit" disabled={state.tag === "submitting"}>
                        {state.tag === "submitting" ? "Creating..." : "Create Channel"}
                    </button>
                </div>
            </fieldset>
            {state.tag === "editing" && state.error && (
                <p style={{ color: "red" }}>{state.error}</p>
            )}
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
                        inputs: state.inputs,
                    };
            }
            break;
        case "redirect":
            throw new Error(
                "Already in final state 'redirect' and should not reduce further."
            );
    }
    return state;
}

type State =
    | { tag: "editing"; error?: string; inputs: { name: string; type: string } }
    | { tag: "submitting"; inputs: { name: string; type: string } }
    | { tag: "redirect" };

type Action =
    | { type: "edit"; inputName: string; inputValue: string }
    | { type: "submit" }
    | { type: "success" }
    | { type: "error"; message: string };

async function createChannel(channelInput: { name: string; type: string; ownerName: string }): Promise<boolean> {
    return fetch("http://localhost:8080/api/channels", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        credentials: "include",
        body: JSON.stringify(channelInput),
    })
        .then((response) => response.ok)
        .catch((err) => {
            console.error(err);
            throw new Error("Failed to create channel");
        });
}
