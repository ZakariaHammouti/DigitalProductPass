import LoginPage from './LoginPage'
import AdminPage from './AdminPage'
import ManufacturerPage from './ManufacturerPage'
import RecyclerPage from './RecyclerPage'
import AuthorityPage from './AuthorityPage'
import {Routes, Route, useNavigate} from "react-router";
import {useEffect} from "react";
import axios from "axios";
//import {AppUser} from "./AppUser.ts";

export default function App() {


    /* useEffect(() => {
         axios.get('/api/auth/me')
             .then(r => {
             })
             .catch((error) => {
                 console.log(error.response?.status, error.response?.data)
             })
     }, [])*/

    return (
        <Routes>
            <Route path="/" element={<LoginPage/>}/>
            <Route path="/admin" element={<AdminPage/>}/>
            <Route path="/manufacturer" element={<ManufacturerPage/>}/>
            <Route path="/recycler" element={<RecyclerPage/>}/>
            <Route path="/authority" element={<AuthorityPage/>}/>
        </Routes>
    )
}