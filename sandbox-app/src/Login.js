/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React, { Component } from 'react';
import Keycloak from 'keycloak-js';
import './App.css';
import { Redirect } from 'react-router';

class Login extends Component {
    constructor(props) {
        super(props);
        this.state = { keycloak: null, authenticated: false };
    }

    componentDidMount() {
        const keycloak = new Keycloak('/keycloak.json');
        keycloak.init({onLoad: 'login-required'}).then(authenticated => {
            var state = { keycloak: keycloak, authenticated: authenticated };
            this.setState(state);
        })
    }

    render() {
        if (this.state.keycloak) {
            if (this.state.authenticated) {
                return (<Redirect to="/" />);
            }
        }
        return (
                <div className="App">
                <h3>Loading...</h3>
                </div>
        );
    }
};

export default Login;
