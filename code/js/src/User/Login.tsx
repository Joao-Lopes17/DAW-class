import * as React from 'react'
import * as ReactDOM from 'react-dom/client'
import { useParams } from 'react-router-dom'
import { Navigate, useLocation } from 'react-router-dom';
import { AuthContext } from './Auth/AuthProvider';

/***********************
 * RequireAuth Component
 */
export function Login() {
    const location = useLocation()
    const {username, setUsername} = React.useContext(AuthContext)
    const [state, dispatch] = React.useReducer(reduce, {
        tag: "editing", inputs: {
            username: "",
            password: ""
        }
    })
    if (state.tag === "redirect") return (
        <Navigate to={location.state?.source} replace={true}></Navigate>
    )
    function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault()
        if (state.tag != "editing") { return }
        dispatch({ type: "submit" })
        const { username, password } = state.inputs
        authenticate(username, password)
            .then(res => { 
                if(res) { setUsername(res) }
                dispatch( res
                    ? {type: "success"}
                    : {type: "error", message: `Invalid username or password: ${username} or ${password}`}
            )})
            .catch(err => dispatch({type: "error", message: err.message}))
    }

    function handleChange(event: React.ChangeEvent<HTMLInputElement>) {
        dispatch({ type: "edit", inputName: event.target.name, inputValue: event.target.value })
    }
    const usr = state.tag === "editing" ? state.inputs.username : ""
    const password = state.tag === "editing" ? state.inputs.password : ""
    return (
        <form onSubmit={handleSubmit}>
            <fieldset disabled={state.tag !== 'editing'}>
            <h3>Login</h3>
                <div>
                    <label htmlFor="username">Username</label>
                    <input id="username" type="text" name="username" value={usr} onChange={handleChange} />
                </div>
                <div>
                    <label htmlFor="password">Password</label>
                    <input id="password" type="text" name="password" value={password} onChange={handleChange} />
                </div>
                <div>
                    <button type="submit">Login</button>
                </div>
            </fieldset>
            {state.tag === 'editing' && state.error}
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
                case "edit": return { tag: 'editing', inputs: { ...state.inputs, [action.inputName]: action.inputValue }, error: undefined }
                case "submit": return { tag: 'submitting' }
            }
        case 'submitting':
            switch (action.type) {
                case "success": return { tag: 'redirect' }
                case "error": return { tag: 'editing', error: action.message, inputs: { username: "", password: "" } }
            }
        case 'redirect':
            throw Error("Already in final State 'redirect' and should not reduce to any other State.")
    }
}

type State = { tag: 'editing'; error?: string, inputs: { username: string, password: string }; }
    | { tag: 'submitting' }
    | { tag: 'redirect' }

type Action = { type: "edit", inputName: string, inputValue: string }
    | { type: "submit" }
    | { type: "success" }
    | { type: "error", message: string }


async function authenticate(username: string, password: string): Promise<string | undefined> {
    try {
        const response = await fetch("http://localhost:8080/api/users/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            body: JSON.stringify({ username, password }),
            
        });

        if (!response.ok) {
            if (response.status === 401 || response.status === 400) {
                return undefined; // Invalid username or password
            }
            throw new Error("Failed to authenticate");
        }

        const data = await response.json();
        return data.username;
    } catch (err) {
        console.error(err);
        throw new Error("Failed to connect to the server");
    }
}

//APENAS PARA TESTE
export function loginTest(){
    const root = ReactDOM.createRoot(document.getElementById('container'))
        // Don't do this => State should be IMMUTABLE => Don't call render
        // ONLY for demo
        root.render(<Login></Login>)

}