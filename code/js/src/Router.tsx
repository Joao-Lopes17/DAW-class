
import * as React from 'react'
import { createRoot } from 'react-dom/client'
import {
    createBrowserRouter, Link, Navigate, Outlet, RouterProvider, useLocation, useParams,
} from 'react-router-dom'
import { AuthContext, AuthProvider } from './User/Auth/AuthProvider'
import { AuthRequire } from './User/Auth/AuthRequire'
import { Login } from './User/Login'
import { Logout } from './User/Logout'
import { Users } from './User/Users'
import { SignUp } from './User/SignUp'
import { UserDetails } from './User/UserDetails'
import { ChannelDetails } from './Channel/ChannelDetails'
import { Channels } from './Channel/Channel'
import { PublicChannels } from './Channel/PublicChannels'
import { CreateChannel } from './Channel/CreateChannel'
import { MessagesInChannel } from './Message/MessagesInChannel'
import { SendMessageToChannel } from './Message/SendMessageToChannel'
import { InvitationDetails } from './Invitation/InvitationDetails'
import { CreateInvitation } from './Invitation/CreateInvitation'
import { AcceptInvitation } from './Invitation/AcceptInvitation'
import { RejectInvitation } from './Invitation/RejectInvitation'
import { Invitations } from './Invitation/Invitations'
import * as ReactDOM from 'react-dom/client'
import { CreateInvitationLink } from './Invitation/CreateInvitationLink'
import { AcceptInvitationRegistration } from './Invitation/AcceptInvitationRegistration'

const router = createBrowserRouter([
    {
        "path": "/",
        "element": 
        
            <AuthProvider>
                <Home />
            </AuthProvider>,
        "children": [

            {
                "path": "signUp",
                "element": <SignUp />,
            },
            {
                "path": "login",
                "element": <Login />,
            },
            {
                "path": "logout",
                "element": 
                <AuthRequire>
                    <Logout />   
                </AuthRequire>,
            },
            {
                "path": "users/",
                "element": 
                <AuthRequire>
                    <Users />,
                </AuthRequire>,
                "children": [
                    {
                        "path": ":userId",
                        "element": 
                        <AuthRequire>
                           <UserDetails />, 
                        </AuthRequire>
                    },

                   
                ]
            },
            {
                "path": "channels/create/",
                "element": 
                <AuthRequire>
                    <CreateChannel />
                    </AuthRequire>
            },
                    {
                        "path": "channels/public/",
                        "element": 
                        <AuthRequire>
                            <PublicChannels />,
                            </AuthRequire>
                    },
            {
                "path": "channels/",
                "element": 
                    <AuthRequire>
                        <Channels />,
                    </AuthRequire>,
                "children": [


                    {
                        "path": ":channelId/",
                        "element": 
                        <AuthRequire>
                            <ChannelDetails />
                        </AuthRequire>,
                        "children": [
                            {
                                "path": "createInvitation",
                                "element": 
                                <AuthRequire>
                                    <CreateInvitation />
                                </AuthRequire>,

                            },
                            {
                                "path": "createInvitationLink",
                                "element": 
                                <AuthRequire>
                                    <CreateInvitationLink />
                                </AuthRequire>,

                            },

                            {
                                "path": "messages/",
                                "element": 
                                <AuthRequire>
                                    <MessagesInChannel />
                                </AuthRequire>,
                                "children": [
                                    {
                                        "path": "send",
                                        "element": 
                                        <AuthRequire>
                                            <SendMessageToChannel />
                                        </AuthRequire>

                                    },
                                ]
                            }
                        ]
                    },
                ]
            },
            {
                "path": "invitation/:code",
                "element": <AcceptInvitationRegistration />
            },
            {
                "path": "invitations/",
                "element": 
                <AuthRequire>
                    <Invitations />
                </AuthRequire>,
                "children": [
                    {
                        "path": ":invitationId/",
                        "element": <InvitationDetails />,
                        "children": [
                            {
                                "path": "accept/:chId/:permission",
                                "element": <AcceptInvitation />,
                            },
                            {
                                "path": "reject",
                                "element": <RejectInvitation />,
                            },
                        ]
                    },

                ]
            }
        ]

    },
])

export function entry() {
    createRoot(document.getElementById("container")).render(
        <RouterProvider router={router} future={{ v7_startTransition: true }} />
    )
}


function Home() {
    const { username } = React.useContext(AuthContext);
    return (
        <div>
            <h1>Home</h1>
            <h3>Username: {username}</h3>
            <ol>
                <li>
                    <Link to="/">Home</Link>
                </li>
                <li>
                    <Link to="/signup">SIGNUP</Link>
                </li>
                <li>
                    <Link to="/login">LOGIN</Link>
                </li>
                <li>
                    <Link to="/logout">LOGOUT</Link>
                </li>
                <li>
                    <Link to="/users">Users</Link>
                </li>
                <li>
                    <Link to="/channels">Channels</Link>
                </li>

                    <ol><Link to="/channels/create">CreateChannel</Link></ol>

                    <ol><Link to="/channels/public">PublicChannels</Link></ol>

                <li>
                    <Link to="/invitations">Invitations</Link>
                </li>
            </ol>
            <Outlet />
        </div>
    );
}


export function homeTest(){
    const root = ReactDOM.createRoot(document.getElementById('container'))
        // Don't do this => State should be IMMUTABLE => Don't call render
        // ONLY for demo
        root.render(<Home></Home>)

}