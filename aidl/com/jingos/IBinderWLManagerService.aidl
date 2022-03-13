package com.jingos;

interface IBinderWLManagerService {
    void StartApp(String package_name);
    void DestroyApp(String package_name);
    void Quit(String reason);

    void Ping(int i);
}
