import * as React from 'react';
import { Navigate, useLocation, useParams } from 'react-router-dom';

export function AcceptInvitationRegistration() {
    const location = useLocation();
    const { code } = useParams();

    const [state, dispatch] = React.useReducer(reduce, {
        tag: "editing",
        inputs: {
            username: "",
            password: ""
        }
    });

    React.useEffect(() => {
        if (state.tag !== "submitting") return;

        const { username, password } = state.inputs;

        fetch(`http://localhost:8080/api/invitations/${code}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password }),
        })
            .then((response) => {
                if (!response.ok) {
                    if (response.status === 404 || response.status === 409 || response.status === 400) {
                        return undefined;
                    }
                    throw new Error("Authentication failed");
                }
                return response.json();
            })
            .catch((err) => dispatch({ type: "error", message: err.message }));
    }, [state]);

    if (state.tag === "redirect") {
        return <Navigate to={location.state?.source || "/"} replace={true} />;
    }

    function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (state.tag !== "editing") return;
        dispatch({ type: "submit" });
    }

    function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
        dispatch({
            type: "edit",
            inputName: event.target.name,
            inputValue: event.target.value
        });
    }

    const usr = state.tag === "editing" ? state.inputs.username : "";
    const password = state.tag === "editing" ? state.inputs.password : "";

    return (
        <form onSubmit={handleSubmit}>
            <fieldset disabled={state.tag !== 'editing'}>
                <h3>Signup via Invite</h3>
                <div>
                    <label htmlFor="username">Username</label>
                    <input id="username" type="text" name="username" value={usr} onChange={handleChange} />
                </div>
                <div>
                    <label htmlFor="password">Password</label>
                    <input id="password" type="text" name="password" value={password} onChange={handleChange} />
                </div>
                <div>
                    <button type="submit">Signup</button>
                </div>
            </fieldset>
            {state.tag === 'editing' && state.error && <p>{state.error}</p>}
        </form>
    );
}

/***********************
 * REDUCER
 */

function reduce(state: State, action: Action): State {
    switch (state.tag) {
        case 'editing':
            switch (action.type) {
                case "edit":
                    return {
                        tag: 'editing',
                        inputs: { ...state.inputs, [action.inputName]: action.inputValue },
                        error: undefined
                    };
                case "submit":
                    return { tag: 'submitting', inputs: state.inputs };
            }
            break;
        case 'submitting':
            switch (action.type) {
                case "success":
                    return { tag: 'redirect' };
                case "error":
                    return {
                        tag: 'editing',
                        error: action.message,
                        inputs: { username: "", password: "" }
                    };
            }
            break;
        case 'redirect':
            throw new Error("Already in final State 'redirect' and should not reduce to any other State.");
    }
    return state;
}

type State = {
    tag: 'editing';
    error?: string;
    inputs: { username: string; password: string };
} | { tag: 'submitting'; inputs: { username: string; password: string } }
    | { tag: 'redirect' };

type Action =
    | { type: "edit"; inputName: string; inputValue: string }
    | { type: "submit" }
    | { type: "success" }
    | { type: "error"; message: string };