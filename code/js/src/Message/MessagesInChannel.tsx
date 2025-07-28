import * as React from 'react';
import { useParams } from 'react-router-dom';
import { SendMessageToChannel } from './SendMessageToChannel';

export type MessageModel = {
    id: number;
    user: {
        id: number;
        username: string;
        passwordValidation: string;
    };
    channel: {
        id: number;
        name: string;
        owner: {
            id: number;
            username: string;
            passwordValidation: string;
        };
        type: string;
    };
    content: string;
    time: string;
};

type State = {
    messages: MessageModel[];
    loading: boolean;
    error?: string;
};

type Action =
    | { type: 'fetch_start' }
    | { type: 'fetch_success'; payload: { messages: MessageModel[] } }
    | { type: 'fetch_error'; error: string }
    | { type: 'add_message'; payload: MessageModel };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case 'fetch_start':
            return { ...state, loading: true, error: undefined };
        case 'fetch_success':
            return { ...state, messages: action.payload.messages, loading: false, error: undefined };
        case 'fetch_error':
            return { ...state, loading: false, error: action.error };
        case 'add_message':
            return { ...state, messages: [...state.messages, action.payload] };
        default:
            throw new Error('Unhandled action type');
    }
}

export function MessagesInChannel() {
    const { channelId } = useParams();
    const [state, dispatch] = React.useReducer(reducer, {
        messages: [],
        loading: false,
    });

    // Fetch inicial
    React.useEffect(() => {
        dispatch({ type: 'fetch_start' });

        fetch(`http://localhost:8080/api/channel/${channelId}/messages`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`Error fetching messages: ${response.statusText}`);
                }
                return response.json();
            })
            .then((data) => {
                dispatch({
                    type: 'fetch_success',
                    payload: { messages: data || [] },
                });
            })
            .catch((err) =>
                dispatch({ type: 'fetch_error', error: err.message || 'Unknown error' })
            );
    }, [channelId]);

    // SSE para mensagens em tempo real
    React.useEffect(() => {
        const eventSource = new EventSource(`http://localhost:8080/api/channel/${channelId}/sse`);

        eventSource.onmessage = (event) => {
            const newMessage: MessageModel = JSON.parse(event.data);
            dispatch({ type: 'add_message', payload: newMessage });
        };

        eventSource.onerror = () => {
            console.error('EventSource connection error');
            eventSource.close();
        };

        return () => {
            eventSource.close();
        };
    }, [channelId]);

    if (state.loading) return <p>Loading messages...</p>;
    if (state.error) return <p>Error: {state.error}</p>;

    return (
        <div>
            <table>
                <thead>
                    <tr>
                        <th>From</th>
                        <th>Channel</th>
                        <th>Message</th>
                        <th>Time</th>
                    </tr>
                </thead>
                <tbody>
                    {state.messages.map((message) => (
                        <tr key={message.id}>
                            <td>{message.user.username}</td>
                            <td>{message.channel.name}</td>
                            <td>{message.content}</td>
                            <td>{new Date(message.time).toLocaleString()}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <div>
                <SendMessageToChannel />
            </div>
        </div>
    );
}




/**
import * as React from 'react';
import { useParams } from 'react-router-dom';
import { SendMessageToChannel } from './SendMessageToChannel';

export type MessageModel = {
    id: number;
    user: {
        id: number;
        username: string;
        passwordValidation: string;
    };
    channel: {
        id: number;
        name: string;
        owner: {
            id: number;
            username: string;
            passwordValidation: string;
        };
        type: string;
    };
    content: string;
    time: string;
};

type State = {
    messages: MessageModel[];
    loading: boolean;
    error?: string;
};

type Action =
    | { type: 'fetch_start' }
    | { type: 'fetch_success'; payload: { messages: MessageModel[] } }
    | { type: 'fetch_error'; error: string };

function reducer(state: State, action: Action): State {
    switch (action.type) {
        case 'fetch_start':
            return { ...state, loading: true, error: undefined };
        case 'fetch_success':
            return { ...state, messages: action.payload.messages, loading: false, error: undefined };
        case 'fetch_error':
            return { ...state, loading: false, error: action.error };
        default:
            throw new Error('Unhandled action type');
    }
}

export function MessagesInChannel() {
    const { channelId } = useParams();
    const [state, dispatch] = React.useReducer(reducer, {
        messages: [],
        loading: false,
    });

    React.useEffect(() => {
        dispatch({ type: 'fetch_start' });

        fetch(`http://localhost:8080/api/channel/${channelId}/messages`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            },
        })
            .then((response) => {
                if (!response.ok) {
                    throw new Error(`Error fetching messages: ${response.statusText}`);
                }
                return response.json();
            })
            .then((data) => {
                console.log('Data received from backend:', data); // Verifique os dados recebidos
                dispatch({
                    type: 'fetch_success',
                    payload: {
                        messages: data || [], // Ajuste para evitar problemas caso `data.messages` não exista
                    },
                });
            })
            .catch((err) =>
                dispatch({ type: 'fetch_error', error: err.message || 'Unknown error' })
            );
    }, [channelId]);

    if (state.loading) return <p>Loading messages...</p>;
    if (state.error) return <p>Error: {state.error}</p>;

    return (
        <div>
            <table>
                <thead>
                    <tr>
                        <th>From</th>
                        <th>Channel</th>
                        <th>Message</th>
                        <th>Time</th>
                    </tr>
                </thead>
                <tbody>
                    {state.messages.map((message) => (
                        <tr key={message.id}>
                            <td>{message.user.username}</td>

                            <td>{message.channel.name}</td>

                            <td>{message.content}</td>

                            <td>{new Date(message.time).toLocaleString()}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
            <div>
                <SendMessageToChannel />
            </div>
        </div>
    );
}
*/