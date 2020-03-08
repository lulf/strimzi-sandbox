import React from 'react';
import Dashboard from './Dashboard.js';
import Register from './Register.js';
import { BrowserRouter, Route, NavLink, Switch } from 'react-router-dom';

function Nav() {
    return (
            <BrowserRouter>
            <Switch>
            <Route exact path="/">
            <div className="App">
            <nav>
            <ul>
            <li>
            <NavLink to="/register">Register</NavLink>
            </li>
            <li>
            <NavLink to="/dashboard">Dashboard</NavLink>
            </li>
            </ul>
            </nav>
            </div>
            </Route>
            <Route path="/register">
            <Register />
            </Route>
            <Route path="/dashboard">
            <Dashboard />
            </Route>
            </Switch>
            </BrowserRouter>
    );
}

export default Nav;
