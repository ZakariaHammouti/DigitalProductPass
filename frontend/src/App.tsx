import LoginPage from './LoginPage'
import AdminPage from './AdminPage'
import ManufacturerPage from './ManufacturerPage'
import RecyclerPage from './RecyclerPage'
import AuthorityPage from './AuthorityPage'
import {Routes, Route} from "react-router";

function App() {

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

export default App