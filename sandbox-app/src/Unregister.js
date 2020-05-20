/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
import React, { Component } from 'react';
import Keycloak from 'keycloak-js';
import './App.css';
import { Redirect } from 'react-router';
import { API_URL } from './constants';

class Unregister extends Component {
    constructor(props) {
        super(props);
        this.state = { keycloak: null, authenticated: false };
    }

    componentDidMount() {
        const keycloak = Keycloak('/keycloak.json');
        keycloak.init({onLoad: 'login-required'}).then(authenticated => {
            var self = this;
            var state = { keycloak: keycloak, authenticated: authenticated, registered: false };
            if (authenticated) {
                var token = keycloak.token;
                keycloak.loadUserProfile().then(function (profile) {
                    fetch(API_URL + '/api/tenants/' + profile.username, {
                        crossDomain: true,
                        method: 'DELETE',
                        headers: {
                            'Accept': 'application/json',
                            'Content-Type': 'application/json',
                            'Authorization': "Bearer " + token,
                        }
                    }).then((response) => {
                        if (response.ok) {
                            state.unregistered = true;
                        } else {
                            state.unregistrationError = response.status;
                        }
                        self.setState(state);
                    }).catch(function () {
                        self.setState(state);
                    });
                }).catch(function() {
                    this.setState(state);
                });
            } else {
                this.setState(state);
            }
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

export default Unregister;
