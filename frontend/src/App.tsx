import LoginPage from './LoginPage'
import AdminPage from './AdminPage'
import ManufacturerPage from './ManufacturerPage'
import RecyclerPage from './RecyclerPage'
import AuthorityPage from './AuthorityPage'
import {Routes, Route, Navigate, Link} from "react-router";
import {useEffect, useState} from "react";
import axios from "axios";
import type {AppUser} from "./AppUser.ts";
import PublicPage from "./Public.tsx";

function startPageFor(role: string) {
    switch (role) {
        case 'ADMIN':
            return '/admin'
        case 'MANUFACTURER':
            return '/manufacturer'
        case 'RECYCLER':
            return '/recycler'
        case 'AUTHORITY':
            return '/authority'
        default:
            return '/'
    }
}

export default function App() {

    const [appUser, setAppUser] = useState<AppUser | null | undefined>(undefined);

    useEffect(() => {
        axios.get('/api/auth/me')
            .then(response =>
                setAppUser(response.data))
            .catch(() => {
                setAppUser(null)
            })
    }, [])

    function handleLogOut() {
        axios.post('/api/auth/logout')
            .then(() => {
                setAppUser(null)
            })
    }

    if (appUser === undefined) return <p>Lade...</p>

    return (
        <>{appUser
            ? <button onClick={handleLogOut}>Abmelden</button>
            : <Link to="/login">Anmelden</Link>}

            <Routes>
                <Route path="/" element={<PublicPage/>}/>

                <Route path="/login" element={appUser ? <Navigate to={startPageFor(appUser.role)} replace/> :
                    <LoginPage setAppUser={setAppUser}/>}/>

                <Route path="/admin" element={
                                         appUser?.role === 'ADMIN' ? <AdminPage/> : <Navigate to="/" replace/>
                                     }/>

                <Route path="/manufacturer" element={
                                                appUser?.role === 'MANUFACTURER' ? <ManufacturerPage/> :
                                                    <Navigate to="/" replace/>
                                            }/>

                <Route path="/recycler" element={
                                            appUser?.role === 'RECYCLER' ? <RecyclerPage/> :
                                                <Navigate to="/" replace/>
                                        }/>

                <Route path="/authority" element={
                                             appUser?.role === 'AUTHORITY' ? <AuthorityPage/> :
                                                 <Navigate to="/" replace/>
                                         }/>
            </Routes>
        </>
    )
}